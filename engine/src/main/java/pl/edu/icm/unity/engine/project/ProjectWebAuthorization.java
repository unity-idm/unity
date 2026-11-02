/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.tx.Transactional;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.project.ProjectAuthorization;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.types.StoredAttribute;

/**
 * Authorizes UpMan web operations using project-local roles
 *
 * @author P.Piernik
 *
 */
@Component
@Primary
class ProjectWebAuthorization implements ProjectAuthorization
{
	private final AttributeDAO attrDao;

	ProjectWebAuthorization(AttributeDAO attrDao)
	{
		this.attrDao = attrDao;
	}

	@Override
	@Transactional
	public void assertManagedProjectsListingAuthorization() throws AuthorizationException
	{
		getClient();
	}

	@Override
	public boolean isProjectVisible(String projectPath)
	{
		return true;
	}

	@Override
	@Transactional
	public void assertManagerAuthorization(String projectPath) throws AuthorizationException
	{
		LoginSession client = getClient();
		assertClientIsProjectManager(projectPath, client.getEntityId());
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

	@Override
	@Transactional
	public void assertProjectsAdminAuthorization(String projectPath) throws AuthorizationException
	{
		LoginSession client = getClient();
		assertClientIsProjectsAdmin(projectPath, client.getEntityId());
	}

	@Override
	@Transactional
	public void assertRoleManagerAuthorization(String projectPath, String groupPath, GroupAuthorizationRole role)
			throws AuthorizationException
	{
		LoginSession client = getClient();
		assertClientCanGiveRole(client.getEntityId(), projectPath, groupPath, role);
	}

	private void assertClientCanGiveRole(long clientId, String projectPath, String groupPath,
			GroupAuthorizationRole role) throws AuthorizationException
	{
		Set<GroupAuthorizationRole> roles = getAuthManagerAttribute(projectPath, clientId);
		if (roles.contains(GroupAuthorizationRole.projectsAdmin))
			return;

		if (roles.contains(GroupAuthorizationRole.manager) && projectPath.equals(groupPath)
				&& !role.equals(GroupAuthorizationRole.projectsAdmin))
			return;

		throw new AuthorizationException(
				"Access is denied. The operation requires manager capability in " + projectPath
						+ " group");
	}

	private void assertClientIsProjectManager(String projectPath, long clientId) throws AuthorizationException
	{
		Set<GroupAuthorizationRole> roles = getAuthManagerAttribute(projectPath, clientId);

		if (!(roles.contains(GroupAuthorizationRole.manager)
				|| roles.contains(GroupAuthorizationRole.projectsAdmin)))
		{
			throw new AuthorizationException(
					"Access is denied. The operation requires manager capability in " + projectPath
							+ " group");
		}
	}

	private void assertClientIsProjectsAdmin(String projectPath, long clientId) throws AuthorizationException
	{
		Set<GroupAuthorizationRole> roles = getAuthManagerAttribute(projectPath, clientId);

		if (!roles.contains(GroupAuthorizationRole.projectsAdmin))
		{
			throw new AuthorizationException(
					"Access is denied. The operation requires tree manager in " + projectPath
							+ " group");
		}
	}

	private Set<GroupAuthorizationRole> getAuthManagerAttribute(String projectPath, long entity)
	{
		List<StoredAttribute> attributes = new ArrayList<>();
		try
		{
			attributes.addAll(attrDao.getAttributes(
					ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE,
					entity, projectPath));

		} catch (Exception e)
		{
			throw new InternalException("Can not get group authorization attribute of entity " + entity);
		}

		Set<GroupAuthorizationRole> roles = new HashSet<>();
		for (StoredAttribute attr : attributes)
		{
			for (String val : attr.getAttribute().getValues())
			{
				roles.add(GroupAuthorizationRole.valueOf(val));
			}
		}
		return roles;
	}
}
