/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.group.delegation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupAuthorizationRole;

/**
 * Authorizes group operations on the engine
 * 
 * @author P.Piernik
 *
 */
@Component
public class GroupAuthorizationManager
{

	private GroupDAO groupDao;
	private AttributeDAO attrDao;

	@Autowired
	public GroupAuthorizationManager(GroupDAO groupDao, AttributeDAO attrDao)
	{
		this.groupDao = groupDao;
		this.attrDao = attrDao;
	}

	@Transactional
	public void checkManagerAuthorization(String groupPath) throws AuthorizationException
	{
		InvocationContext authnCtx = InvocationContext.getCurrent();
		LoginSession client = authnCtx.getLoginSession();

		if (client == null)
			throw new AuthorizationException(
					"Access is denied. The client is not authenticated.");

		if (client.isUsedOutdatedCredential())
		{

			throw new AuthorizationException(
					"Access is denied. The client's credential "
							+ "is outdated and the only allowed operation is the credential update");
		}

		if (!checkIfDelegationIsActive(groupPath))
		{
			throw new AuthorizationException(
					"Access is denied. The operation requires enabled delegation in path "
							+ groupPath + " group");
		}

		if (!checkAuthManagerAttribute(groupPath, client.getEntityId()))
		{
			throw new AuthorizationException(
					"Access is denied. The operation requires manager capability in path "
							+ groupPath);
		}
	}

	private boolean checkAuthManagerAttribute(String groupPath, long entity)
	{
		List<StoredAttribute> attributes = new ArrayList<>();
		try
		{
			attributes.addAll(attrDao.getAttributes(
					GroupAuthorizationRoleAttributeTypeProvider.GROUP_AUTHORIZATION_ROLE,
					entity, groupPath));

		} catch (Exception e)
		{
			// ok
		}

		for (StoredAttribute attr : attributes)
		{
			for (String val : attr.getAttribute().getValues())
			{
				if (val.equals(GroupAuthorizationRole.manager.toString()))
					return true;
			}
		}

		return false;

	}

	private boolean checkIfDelegationIsActive(String groupPath)
	{
		try
		{
			Group group = groupDao.get(groupPath);
			return group.getDelegationConfiguration().isEnabled();
		} catch (Exception e)
		{
			throw new IllegalArgumentException(
					"Group " + groupPath + " does not exist");
		}
	}

}
