/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Authorizes group operations on the engine
 * 
 * @author P.Piernik
 *
 */
@Component
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
	public void checkManagerAuthorization(String projectPath) throws AuthorizationException
	{

		LoginSession client = getClient();
		assertIfDelegationIsActive(projectPath);
		assertIfClientIsProjectManager(projectPath, client.getEntityId());
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
	public void checkManagerAuthorization(String projectPath, String groupPath) throws AuthorizationException
	{

		checkManagerAuthorization(projectPath);
		assertGroupIsUnderProject(projectPath, groupPath);

	}

	@Transactional
	public void checkDelegationManagerAuthorizationWithoutGroupDelegationVerification(String projectPath, String groupPath)
			throws AuthorizationException

	{
		LoginSession client = getClient();
		assertIfDelegationIsActive(projectPath);
		assertGroupIsUnderProject(projectPath, groupPath);
		assertIfClientIsProjectManagerWithRedelegationPrivilages(projectPath, groupPath, client.getEntityId());

	}
	
	@Transactional
	public void checkDelegationManagerAuthorization(String projectPath, String groupPath)
			throws AuthorizationException

	{
		assertIfDelegationIsActive(groupPath);
		checkDelegationManagerAuthorizationWithoutGroupDelegationVerification(projectPath, groupPath);

	}

	private void assertIfDelegationIsActive(String projectPath) throws AuthorizationException
	{
		if (!checkIfDelegationIsActive(projectPath))
		{
			throw new AuthorizationException(
					"Access is denied. The operation requires enabled delegation on " + projectPath
							+ " group");
		}
	}

	private boolean checkIfDelegationIsActive(String projectPath)
	{
		try
		{
			Group group = groupDao.get(projectPath);
			return group.getDelegationConfiguration().enabled;
		} catch (Exception e)
		{
			throw new InternalException("Can not get group " + projectPath);
		}
	}

	private void assertIfClientIsProjectManager(String projectPath, long clientId) throws AuthorizationException
	{
		Set<GroupAuthorizationRole> roles = getAuthManagerAttribute(projectPath, clientId);

		if (!(roles.contains(GroupAuthorizationRole.manager)
				|| roles.contains(GroupAuthorizationRole.allowReDelegate)
				|| roles.contains(GroupAuthorizationRole.allowReDelegateRecursive)))
		{
			throw new AuthorizationException(
					"Access is denied. The operation requires manager capability in " + projectPath
							+ " group");
		}
	}

	private void assertIfClientIsProjectManagerWithRedelegationPrivilages(String projectPath, String groupPath,
			long clientId) throws AuthorizationException
	{
		Set<GroupAuthorizationRole> roles = getAuthManagerAttribute(projectPath, clientId);

		if (roles.contains(GroupAuthorizationRole.allowReDelegateRecursive))
		{
			return;
		}
		
		if (roles.contains(GroupAuthorizationRole.allowReDelegate) && Group.isDirectChild(groupPath, projectPath))
		{
			return;
		}
		
		throw new AuthorizationException(
				"Access is denied. The operation requires manager with redelegation capability in " + projectPath
						+ " group");
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
