/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.tx.Transactional;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.engine.api.project.RestGroupAuthorizationRole;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.imunity.upman.rest.ProjectManagerRestRoleAttributeTypeProvider.AUTHORIZATION_ROLE;

@Component
class UpmanRestAuthorizationManager
{
	private final AttributesManagement attrDao;

	@Autowired
	public UpmanRestAuthorizationManager(@Qualifier("insecure") AttributesManagement attrDao)
	{
		this.attrDao = attrDao;
	}

	@Transactional
	public void assertManagerAuthorization(String authorizationGroupPath) throws AuthorizationException
	{
		LoginSession client = getClient();
		if (isOAuthRequest())
		{
			assertScope(UpmanSystemScopeProvider.API_SCOPE);
			return;
		}
		assertClientIsProjectManager(authorizationGroupPath, client.getEntityId());
	}

	@Transactional
	public void assertProjectAuthorization(String authorizationGroupPath, String projectId) throws AuthorizationException
	{
		LoginSession client = getClient();
		if (isOAuthRequest())
		{
			String canonicalId = ProjectPathProvider.relativeProjectId(
					ProjectPathProvider.getProjectPath(projectId, "/"), "/");
			if (!canAccessProject(canonicalId))
				throw new AuthorizationException("Access is denied. The token has no scope for this project.");
			return;
		}
		assertClientIsProjectManager(authorizationGroupPath, client.getEntityId());
	}

	@Transactional
	public void assertAnyProjectAuthorization(String authorizationGroupPath) throws AuthorizationException
	{
		LoginSession client = getClient();
		if (isOAuthRequest())
		{
			if (!hasAnyProjectScope())
				throw new AuthorizationException("Access is denied. The token has no UpMan API scope.");
			return;
		}
		assertClientIsProjectManager(authorizationGroupPath, client.getEntityId());
	}

	boolean canAccessProject(String projectId)
	{
		if (!isOAuthRequest())
			return true;
		List<String> scopes = InvocationContext.getCurrent().getScopes();
		return scopes.contains(UpmanSystemScopeProvider.API_SCOPE)
				|| scopes.contains(UpmanSystemScopeProvider.PROJECT_SCOPE_PREFIX + projectId);
	}

	private boolean hasAnyProjectScope()
	{
		return InvocationContext.getCurrent().getScopes().stream()
				.anyMatch(scope -> scope.equals(UpmanSystemScopeProvider.API_SCOPE)
						|| scope.startsWith(UpmanSystemScopeProvider.PROJECT_SCOPE_PREFIX)
						&& scope.length() > UpmanSystemScopeProvider.PROJECT_SCOPE_PREFIX.length());
	}

	private void assertScope(String scope) throws AuthorizationException
	{
		if (!InvocationContext.getCurrent().getScopes().contains(scope))
			throw new AuthorizationException("Access is denied. The token has no " + scope + " scope.");
	}

	private boolean isOAuthRequest()
	{
		return InvocationContext.getCurrent().getInvocationMaterial() == InvocationMaterial.OAUTH_DELEGATION;
	}

	private LoginSession getClient() throws AuthorizationException
	{
		InvocationContext authnCtx = InvocationContext.getCurrent();
		LoginSession client = authnCtx.getLoginSession();

		if (client == null)
			throw new AuthorizationException("Access is denied. The client is not authenticated.");

		if (client.isUsedOutdatedCredential())
		{

			throw new AuthorizationException("Access is denied. The client's credential "
					+ "is outdated and the only allowed operation is the credential update");
		}
		return client;
	}

	private void assertClientIsProjectManager(String authorizationPath, long clientId) throws AuthorizationException
	{
		Set<RestGroupAuthorizationRole> roles = getAuthManagerAttribute(authorizationPath, clientId);

		if (!roles.contains(RestGroupAuthorizationRole.manager))
		{
			throw new AuthorizationException(
					"Access is denied. The operation requires project management RESTAPI Role”");
		}
	}

	private Set<RestGroupAuthorizationRole> getAuthManagerAttribute(String authorizationPath, long entity) throws AuthorizationException
	{
		List<AttributeExt> attributes;
		try
		{
			attributes = new ArrayList<>(
				attrDao.getAttributes(
					new EntityParam(entity),
					authorizationPath,
					AUTHORIZATION_ROLE)
			);

		} catch (EngineException e)
		{
			throw new AuthorizationException(
				"Access is denied. The operation requires user [" + entity + "] to be a member of the " + authorizationPath
					+ " group");
		}

		Set<RestGroupAuthorizationRole> roles = new HashSet<>();
		for (AttributeExt attr : attributes)
		{
			for (String val : attr.getValues())
			{
				roles.add(RestGroupAuthorizationRole.valueOf(val));
			}
		}
		return roles;
	}
}
