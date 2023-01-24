/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
		assertClientIsProjectManager(authorizationGroupPath, client.getEntityId());
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
		Set<GroupAuthorizationRole> roles = getAuthManagerAttribute(authorizationPath, clientId);

		if (!(roles.contains(GroupAuthorizationRole.manager)
				|| roles.contains(GroupAuthorizationRole.projectsAdmin)))
		{
			throw new AuthorizationException(
					"Access is denied. The operation requires unity rest manager capability in " + authorizationPath
							+ " group");
		}
	}

	private Set<GroupAuthorizationRole> getAuthManagerAttribute(String authorizationPath, long entity) throws AuthorizationException
	{
		List<AttributeExt> attributes;
		try
		{
			attributes = new ArrayList<>(
				attrDao.getAttributes(
					new EntityParam(entity),
					authorizationPath,
				"sys:ProjectManagementRESTAPIRole")
			);

		} catch (EngineException e)
		{
			throw new AuthorizationException(
				"Access is denied. The operation requires user " + entity + " belongs to " + authorizationPath
					+ " group");
		}

		Set<GroupAuthorizationRole> roles = new HashSet<>();
		for (AttributeExt attr : attributes)
		{
			for (String val : attr.getValues())
			{
				roles.add(GroupAuthorizationRole.valueOf(val));
			}
		}
		return roles;
	}
}
