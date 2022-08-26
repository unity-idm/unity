/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.admin;

import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupMembership;

class AdminAuthzService
{

	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, AdminAuthzService.class);

	private final SCIMEndpointDescription configuration;
	private final EntityManagement entityManagement;

	AdminAuthzService(SCIMEndpointDescription configuration, EntityManagement entityManagement)
	{
		this.configuration = configuration;
		this.entityManagement = entityManagement;
	}

	void checkUpdateExposedGroups() throws EngineException
	{
		if (configuration.restAdminGroup.isEmpty())
		{
			log.debug("Missconfigured SCIM endpoint {}, rest admin group is not set", configuration.endpointName);
			throw new AuthorizationException("Access is denied");
		}

		InvocationContext invocationContext = InvocationContext.getCurrent();
		if (!invocationContext.getInvocationMaterial().equals(InvocationMaterial.DIRECT))
		{
			log.debug("Access is denied. Update exposed groups is available only via direct access");
			throw new AuthorizationException("Access is denied");
		}

		Map<String, GroupMembership> userGroups = entityManagement
				.getGroups(new EntityParam(invocationContext.getLoginSession().getEntityId()));

		if (!userGroups.containsKey(configuration.restAdminGroup.get()))
		{
			log.debug("Access is denied. Caller not a member of admin SCIM group");
			throw new AuthorizationException("Access is denied");
		}
	}

	@Component
	static class SCIMAdminAuthzServiceFactory
	{
		private final EntityManagement entityManagement;

		@Autowired
		SCIMAdminAuthzServiceFactory(@Qualifier("insecure") EntityManagement entityManagement)
		{

			this.entityManagement = entityManagement;
		}

		AdminAuthzService getService(SCIMEndpointDescription configuration)
		{
			return new AdminAuthzService(configuration, entityManagement);
		}
	}

}
