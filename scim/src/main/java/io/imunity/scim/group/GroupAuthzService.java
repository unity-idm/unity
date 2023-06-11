/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

import java.util.Map;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.scim.SCIMSystemScopeProvider;
import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;

class GroupAuthzService
{
	private final AuthorizationManagement authzMan;
	private final SCIMEndpointDescription configuration;
	private final EntityManagement entityManagement;

	GroupAuthzService(AuthorizationManagement authzMan, EntityManagement entityManagement,
			SCIMEndpointDescription configuration)
	{
		this.authzMan = authzMan;
		this.configuration = configuration;
		this.entityManagement = entityManagement;
	}

	void checkReadGroups() throws AuthorizationException
	{
		InvocationContext invocationContext = InvocationContext.getCurrent();
		if (invocationContext.getInvocationMaterial().equals(InvocationMaterial.OAUTH_DELEGATION))
		{
			if (!invocationContext.getScopes().contains(SCIMSystemScopeProvider.READ_SELF_GROUP_SCOPE))
				throw new AuthorizationException(
						"Access is denied. Reading groups over OAuth is available only with scope "
								+ SCIMSystemScopeProvider.READ_SELF_GROUP_SCOPE);
		}else {
			authzMan.checkReadCapability(false, configuration.rootGroup);
		}
	}

	Predicate<String> getFilter() throws EngineException
	{
		InvocationContext current = InvocationContext.getCurrent();
		if (current.getInvocationMaterial().equals(InvocationMaterial.DIRECT))
		{
			return s -> true;
		} else
		{
			Map<String, GroupMembership> userGroups = entityManagement
					.getGroups(new EntityParam(current.getLoginSession().getEntityId()));
			return s -> userGroups.keySet().contains(s);
		}
	}

	@Component
	static class SCIMGroupAuthzServiceFactory
	{
		private final AuthorizationManagement authzMan;
		private final EntityManagement entityManagement;

		@Autowired
		SCIMGroupAuthzServiceFactory(AuthorizationManagement authzMan, EntityManagement entityManagement)
		{
			this.authzMan = authzMan;
			this.entityManagement = entityManagement;
		}

		GroupAuthzService getService(SCIMEndpointDescription configuration)
		{
			return new GroupAuthzService(authzMan, entityManagement, configuration);
		}
	}
}
