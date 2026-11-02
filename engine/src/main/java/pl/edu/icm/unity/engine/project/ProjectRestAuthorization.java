/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import java.util.List;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.project.RestGroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.project.RestProjectAuthorization;

final class ProjectRestAuthorization implements RestProjectAuthorization
{
	private final RestProjectAuthorization oauthScopeAuthorization;
	private final RestProjectAuthorization restApiRoleAuthorization;

	ProjectRestAuthorization(AttributesManagement attributesManagement, String projectsRootPath,
			String apiUsersGroupPath)
	{
		oauthScopeAuthorization = new OAuthScopeAuthorization(projectsRootPath);
		restApiRoleAuthorization = new RestApiRoleAuthorization(attributesManagement, projectsRootPath, apiUsersGroupPath);
	}

	@Override
	public void assertProjectCreationAuthorization() throws AuthorizationException
	{
		authorizationForAuthenticatedCaller().assertProjectCreationAuthorization();
	}

	@Override
	public void assertManagerAuthorization(String projectPath) throws AuthorizationException
	{
		authorizationForAuthenticatedCaller().assertManagerAuthorization(projectPath);
	}

	@Override
	public void assertProjectsDiscoveryAuthorization() throws AuthorizationException
	{
		authorizationForAuthenticatedCaller().assertProjectsDiscoveryAuthorization();
	}

	@Override
	public boolean isProjectVisible(String projectPath)
	{
		return selectAuthorization().isProjectVisible(projectPath);
	}

	@Override
	public void assertManagedProjectsListingAuthorization() throws AuthorizationException
	{
		authorizationForAuthenticatedCaller().assertManagedProjectsListingAuthorization();
	}

	@Override
	public void assertProjectsAdminAuthorization(String projectPath) throws AuthorizationException
	{
		authorizationForAuthenticatedCaller().assertProjectsAdminAuthorization(projectPath);
	}

	@Override
	public void assertRoleManagerAuthorization(String projectPath, String roleAssignmentGroupPath,
			GroupAuthorizationRole assignedRole)
			throws AuthorizationException
	{
		authorizationForAuthenticatedCaller().assertRoleManagerAuthorization(
				projectPath, roleAssignmentGroupPath, assignedRole);
	}

	private RestProjectAuthorization authorizationForAuthenticatedCaller() throws AuthorizationException
	{
		LoginSession callerSession = InvocationContext.getCurrent().getLoginSession();
		if (callerSession == null)
			throw new AuthorizationException("Access is denied. The client is not authenticated.");
		if (callerSession.isUsedOutdatedCredential())
			throw new AuthorizationException("Access is denied. The client's credential "
					+ "is outdated and the only allowed operation is the credential update");
		return selectAuthorization();
	}

	private RestProjectAuthorization selectAuthorization()
	{
		return switch (InvocationContext.getCurrent().getInvocationMaterial())
		{
			case OAUTH_DELEGATION -> oauthScopeAuthorization;
			case DIRECT -> restApiRoleAuthorization;
		};
	}

	private static final class OAuthScopeAuthorization implements RestProjectAuthorization
	{
		private final String projectsRootPath;

		private OAuthScopeAuthorization(String projectsRootPath)
		{
			this.projectsRootPath = projectsRootPath;
		}

		@Override
		public void assertProjectCreationAuthorization() throws AuthorizationException
		{
			if (!InvocationContext.getCurrent().getScopes().contains(API_SCOPE))
				throw new AuthorizationException("Access is denied. The token has no " + API_SCOPE + " scope.");
		}

		@Override
		public void assertManagerAuthorization(String projectPath) throws AuthorizationException
		{
			if (!isUnderProjectsRoot(projectPath))
				throw new AuthorizationException("Project is outside the configured root");
			if (!hasScopeForProject(projectPath))
				throw new AuthorizationException("Access is denied. The token has no scope for this project.");
		}

		@Override
		public void assertProjectsDiscoveryAuthorization() throws AuthorizationException
		{
			boolean hasDiscoveryScope = InvocationContext.getCurrent().getScopes().stream()
					.anyMatch(tokenScope -> tokenScope.equals(API_SCOPE)
							|| tokenScope.startsWith(PROJECT_SCOPE_PREFIX)
							&& tokenScope.length() > PROJECT_SCOPE_PREFIX.length());
			if (!hasDiscoveryScope)
				throw new AuthorizationException("Access is denied. The token has no UpMan API scope.");
		}

		@Override
		public boolean isProjectVisible(String projectPath)
		{
			return isUnderProjectsRoot(projectPath) && hasScopeForProject(projectPath);
		}

		@Override
		public void assertManagedProjectsListingAuthorization() throws AuthorizationException
		{
			assertProjectsDiscoveryAuthorization();
		}

		@Override
		public void assertProjectsAdminAuthorization(String projectPath) throws AuthorizationException
		{
			assertManagerAuthorization(projectPath);
		}

		@Override
		public void assertRoleManagerAuthorization(String projectPath, String roleAssignmentGroupPath,
				GroupAuthorizationRole assignedRole)
				throws AuthorizationException
		{
			assertManagerAuthorization(projectPath);
		}

		private boolean hasScopeForProject(String projectPath)
		{
			String rootRelativeProjectId = projectPath.substring(
					projectsRootPath.equals("/") ? 1 : projectsRootPath.length() + 1);
			List<String> tokenScopes = InvocationContext.getCurrent().getScopes();
			return tokenScopes.contains(API_SCOPE) || tokenScopes.contains(PROJECT_SCOPE_PREFIX + rootRelativeProjectId);
		}

		private boolean isUnderProjectsRoot(String projectPath)
		{
			return !projectsRootPath.equals(projectPath) && Group.isChildOrSame(projectPath, projectsRootPath);
		}
	}

	private static final class RestApiRoleAuthorization implements RestProjectAuthorization
	{
		private final AttributesManagement attributesManagement;
		private final String projectsRootPath;
		private final String apiUsersGroupPath;

		private RestApiRoleAuthorization(AttributesManagement attributesManagement, String projectsRootPath,
				String apiUsersGroupPath)
		{
			this.attributesManagement = attributesManagement;
			this.projectsRootPath = projectsRootPath;
			this.apiUsersGroupPath = apiUsersGroupPath;
		}

		@Override
		public void assertProjectCreationAuthorization() throws AuthorizationException
		{
			assertApiManagerRole();
		}

		@Override
		public void assertManagerAuthorization(String projectPath) throws AuthorizationException
		{
			if (!isProjectVisible(projectPath))
				throw new AuthorizationException("Project is outside the configured root");
			assertApiManagerRole();
		}

		@Override
		public void assertProjectsDiscoveryAuthorization() throws AuthorizationException
		{
			assertApiManagerRole();
		}

		@Override
		public boolean isProjectVisible(String projectPath)
		{
			return !projectsRootPath.equals(projectPath) && Group.isChildOrSame(projectPath, projectsRootPath);
		}

		@Override
		public void assertManagedProjectsListingAuthorization() throws AuthorizationException
		{
			assertProjectsDiscoveryAuthorization();
		}

		@Override
		public void assertProjectsAdminAuthorization(String projectPath) throws AuthorizationException
		{
			assertManagerAuthorization(projectPath);
		}

		@Override
		public void assertRoleManagerAuthorization(String projectPath, String roleAssignmentGroupPath,
				GroupAuthorizationRole assignedRole)
				throws AuthorizationException
		{
			assertManagerAuthorization(projectPath);
		}

		private void assertApiManagerRole() throws AuthorizationException
		{
			long callerEntityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
			boolean hasApiManagerRole;
			try
			{
				hasApiManagerRole = attributesManagement.getAttributes(
						new EntityParam(callerEntityId), apiUsersGroupPath, AUTHORIZATION_ROLE)
						.stream().anyMatch(attribute -> attribute.getValues()
								.contains(RestGroupAuthorizationRole.manager.name()));
			} catch (EngineException e)
			{
				throw new AuthorizationException("Access is denied. The operation requires user [" + callerEntityId
						+ "] to be a member of the " + apiUsersGroupPath + " group");
			}
			if (!hasApiManagerRole)
				throw new AuthorizationException(
						"Access is denied. The operation requires project management REST API role");
		}
	}
}
