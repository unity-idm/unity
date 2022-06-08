/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.exceptions.AuthorizationException;

class GroupAuthzService
{
	private final AuthorizationManagement authzMan;
	private final SCIMEndpointDescription configuration;

	GroupAuthzService(AuthorizationManagement authzMan, SCIMEndpointDescription configuration)
	{
		this.authzMan = authzMan;
		this.configuration = configuration;
	}

	void checkReadGroups() throws AuthorizationException
	{
		InvocationContext invocationContext = InvocationContext.getCurrent();
		if (!invocationContext.getInvocationMaterial().equals(InvocationMaterial.DIRECT))
		{
			throw new AuthorizationException("Access is denied. Reading groups is available only via direct access");
		}	
		authzMan.checkReadCapability(false, configuration.rootGroup);
	}

	@Component
	static class SCIMGroupAuthzServiceFactory
	{
		private final AuthorizationManagement authzMan;

		@Autowired
		SCIMGroupAuthzServiceFactory(AuthorizationManagement authzMan)
		{
			this.authzMan = authzMan;
		}

		GroupAuthzService getService(SCIMEndpointDescription configuration)
		{
			return new GroupAuthzService(authzMan, configuration);
		}
	}
}
