/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import static io.imunity.upman.rest.ProjectManagerRestRoleAttributeTypeProvider.AUTHORIZATION_ROLE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.RestGroupAuthorizationRole;

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
