/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredAttribute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Authorizes group operations on the engine
 * 
 * @author P.Piernik
 *
 */
@Component
@Primary
public class ProjectAuthorizationManager
{

	private GroupDAO groupDao;
	private AttributeDAO attrDao;

	@Autowired
	public ProjectAuthorizationManager(GroupDAO groupDao, AttributeDAO attrDao)
	{
		this.groupDao = groupDao;
		this.attrDao = attrDao;
	}

	@Transactional
	public void assertManagerAuthorization(String projectPath) throws AuthorizationException
	{

		LoginSession client = getClient();
		assertDelegationIsEnabled(projectPath);
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

	@Transactional
	public void assertManagerAuthorization(String projectPath, String groupPath) throws AuthorizationException
	{

		assertManagerAuthorization(projectPath);
		assertGroupIsUnderProject(projectPath, groupPath);

	}

	@Transactional
	public void assertProjectsAdminAuthorization(String projectPath, String groupPath)
			throws AuthorizationException

	{
		LoginSession client = getClient();
		assertDelegationAndSubprojectsAreEnabled(projectPath);
		assertGroupIsUnderProject(projectPath, groupPath);
		assertClientIsProjectsAdmin(projectPath, groupPath, client.getEntityId());

	}
	
	@Transactional
	public void assertRoleManagerAuthorization(String projectPath, String groupPath, GroupAuthorizationRole role)
			throws AuthorizationException

	{
		LoginSession client = getClient();
		assertDelegationIsEnabled(projectPath);
		assertDelegationIsEnabled(groupPath);
		assertGroupIsUnderProject(projectPath, groupPath);
		assertClientCanGiveRole(client.getEntityId(), projectPath, groupPath, role);

	}

	private void assertClientCanGiveRole(long clientId, String projectPath, String groupPath, GroupAuthorizationRole role) throws AuthorizationException
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

	private void assertDelegationIsEnabled(String projectPath) throws AuthorizationException
	{
		if (!getGroup(projectPath).getDelegationConfiguration().enabled)
		{
			throw new AuthorizationException(
					"Access is denied. The operation requires enabled delegation on " + projectPath
							+ " group");
		}
	}
	
	private void assertDelegationAndSubprojectsAreEnabled(String projectPath) throws AuthorizationException
	{
		GroupDelegationConfiguration config = getGroup(projectPath).getDelegationConfiguration();
		if (!config.enabled || !config.enableSubprojects)
		{
			throw new AuthorizationException(
					"Access is denied. The operation requires enabled delegation and subprojects creation on " + projectPath
							+ " group");
		}
	}

	private Group getGroup(String projectPath)
	{
		try
		{
			return groupDao.get(projectPath);
		} catch (Exception e)
		{
			throw new InternalException("Can not get group " + projectPath);
		}
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

	private void assertClientIsProjectsAdmin(String projectPath, String groupPath,
			long clientId) throws AuthorizationException
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
					ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE
							.toString(),
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

	private void assertGroupIsUnderProject(String projectPath, String childPath)
			throws NotChildOfProjectGroupException
	{
		if (!Group.isChildOrSame(childPath, projectPath))
			throw new NotChildOfProjectGroupException(projectPath, childPath);
	}

	private static class NotChildOfProjectGroupException extends RuntimeException
	{
		public NotChildOfProjectGroupException(String parent, String child)
		{
			super("Group " + child + " is not child of main project group " + parent);
		}
	}

}
