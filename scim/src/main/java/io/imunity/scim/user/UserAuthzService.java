/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.util.Set;
import java.util.function.Predicate;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.scim.SCIMSystemScopeProvider;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.exceptions.AuthorizationException;

class UserAuthzService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, UserAuthzService.class);

	private final AuthorizationManagement authzMan;
	private final SCIMEndpointDescription configuration;

	UserAuthzService(AuthorizationManagement authzMan, SCIMEndpointDescription configuration)
	{
		this.authzMan = authzMan;
		this.configuration = configuration;
	}

	void checkReadUser(long entity, Set<String> userGroups) throws AuthorizationException
	{
		InvocationContext invocationContext = InvocationContext.getCurrent();

		switch (invocationContext.getInvocationMaterial())
		{
		case DIRECT:
			checkReadUserWithDirectInvocationMaterial(entity, invocationContext, userGroups);
			break;
		case OAUTH_DELEGATION:
			checkReadUserWithOAuthInvocationMaterial(entity, invocationContext, userGroups);
			break;
		default:
			throw new AuthorizationException("Access is denied");
		}
	}

	private void checkReadUserWithDirectInvocationMaterial(long entity, InvocationContext invocationContext,
			Set<String> userGroups) throws AuthorizationException
	{
		long callerId = invocationContext.getLoginSession().getEntityId();

		try
		{
			authzMan.checkReadCapability(entity == callerId, configuration.rootGroup);
		} catch (AuthorizationException e)
		{
			if (entity == callerId && userGroups.contains(configuration.rootGroup))
				return;
			else
			{
				log.debug("Access is denied. Caller not a member of root SCIM group");
				throw new AuthorizationException("Access is denied");
			}
		}
	}

	private void checkReadUserWithOAuthInvocationMaterial(long entity, InvocationContext invocationContext,
			Set<String> userGroups) throws AuthorizationException
	{
		long callerId = invocationContext.getLoginSession().getEntityId();
		if (entity != callerId)
		{
			log.debug("Access is denied. Caller wants to read data that is not his own");
			throw new AuthorizationException("Access is denied");
		}

		if (!userGroups.contains(configuration.rootGroup))
		{
			log.debug("Access is denied. Caller not a member of root SCIM group");
			throw new AuthorizationException("Access is denied");
		}

		if (!invocationContext.getScopes().stream().anyMatch(SCIMSystemScopeProvider.getScopeNames()::contains))
		{
			log.debug("Access is denied. Client does not have the required scopes");
			throw new AuthorizationException("Access is denied");
		}
	}

	void checkReadUsers() throws AuthorizationException
	{
		if (InvocationContext.getCurrent().getInvocationMaterial().equals(InvocationMaterial.DIRECT))
		{
			authzMan.checkReadCapability(false, configuration.rootGroup);
		} else
		{
			log.debug("Access is denied. Reading users is available only via direct access");
			throw new AuthorizationException("Access is denied");
		}
	}

	Predicate<AttributeDefinitionWithMapping> getFilter()
	{
		InvocationContext current = InvocationContext.getCurrent();
		if (current.getInvocationMaterial().equals(InvocationMaterial.DIRECT))
		{
			return s -> true;
		} else
		{
			Predicate<AttributeDefinitionWithMapping> attributeFilter = s -> false;
			if (current.getScopes().contains(SCIMSystemScopeProvider.READ_PROFILE_SCOPE))
				attributeFilter = attributeFilter
						.or(s -> !configuration.membershipAttributes.contains(s.attributeDefinition.name));
			if (current.getScopes().contains(SCIMSystemScopeProvider.READ_MEMBERSHIPS_SCOPE))
				attributeFilter = attributeFilter
						.or(s -> configuration.membershipAttributes.contains(s.attributeDefinition.name));
			return attributeFilter;
		}
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

		UserAuthzService getService(SCIMEndpointDescription configuration)
		{
			return new UserAuthzService(authzMan, configuration);
		}
	}
}
