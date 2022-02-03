/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.exceptions.AuthorizationException;

class SCIMUserAuthzService
{
	private final AuthorizationManagement authzMan;
	private final SCIMEndpointDescription configuration;

	SCIMUserAuthzService(AuthorizationManagement authzMan, SCIMEndpointDescription configuration)
	{
		this.authzMan = authzMan;
		this.configuration = configuration;
	}

	void checkReadUser(long entity) throws AuthorizationException
	{
		long callerId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		authzMan.checkReadCapability(entity == callerId, configuration.rootGroup);
	}

	void checkReadUsers() throws AuthorizationException
	{
		authzMan.checkReadCapability(false, configuration.rootGroup);
	}

	@Component
	static class SCIMUserAuthzServiceFactory
	{
		private final AuthorizationManagement authzMan;

		@Autowired
		SCIMUserAuthzServiceFactory(AuthorizationManagement authzMan)
		{
			this.authzMan = authzMan;
		}

		SCIMUserAuthzService getService(SCIMEndpointDescription configuration)
		{
			return new SCIMUserAuthzService(authzMan, configuration);
		}
	}
}
