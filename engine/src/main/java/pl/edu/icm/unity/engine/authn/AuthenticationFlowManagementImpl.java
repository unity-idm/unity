/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.authn;

import java.util.Collection;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.capacity_limit.CapacityLimitName;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.capacityLimits.InternalCapacityLimitVerificator;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorConfigurationDB;
import pl.edu.icm.unity.base.tx.Transactional;

/**
 * Authentication flow management implementation.
 * @author P.Piernik
 */
@Component
@Primary
@InvocationEventProducer
@Transactional
public class AuthenticationFlowManagementImpl implements AuthenticationFlowManagement
{

	private AuthenticationFlowDB authnFlowDB;
	private InternalAuthorizationManager authz;
	private AuthenticatorConfigurationDB authenticatorDB;
	private InternalCapacityLimitVerificator capacityLimitVerificator;
	
	@Autowired
	public AuthenticationFlowManagementImpl(AuthenticationFlowDB authnFlowDB,
			InternalAuthorizationManager authz, AuthenticatorConfigurationDB authenticatorDB, 
			InternalCapacityLimitVerificator capacityLimit)
	{
		
		this.authnFlowDB = authnFlowDB;
		this.authz = authz;
		this.authenticatorDB = authenticatorDB;
		this.capacityLimitVerificator = capacityLimit;
	}

	
	@Override
	public void addAuthenticationFlow(AuthenticationFlowDefinition authFlowdef) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		capacityLimitVerificator.assertInSystemLimitForSingleAdd(CapacityLimitName.AuthenticationFlowsCount,
				() -> authnFlowDB.getCount());

		if (authenticatorDB.getAllAsMap().get(authFlowdef.getName()) != null)
		{
			throw new IllegalArgumentException(
					"Can not add authentication flow " + authFlowdef.getName()
							+ ", authenticator with the same name exists");
		}
		
		assertIfAuthenticatorsExists(authFlowdef.getAllAuthenticators(),
				authFlowdef.getName());
		authFlowdef.setRevision(0);
		authnFlowDB.create(authFlowdef);	
	}

	@Override
	public void removeAuthenticationFlow(String toRemove) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		authnFlowDB.delete(toRemove);
		
	}

	@Override
	public Collection<AuthenticationFlowDefinition> getAuthenticationFlows() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return authnFlowDB.getAll();
	}
	
	@Override
	public AuthenticationFlowDefinition getAuthenticationFlow(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return authnFlowDB.get(name);
	}

	@Override
	public void updateAuthenticationFlow(AuthenticationFlowDefinition authFlowdef) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIfAuthenticatorsExists(authFlowdef.getAllAuthenticators(),
				authFlowdef.getName());
		
		AuthenticationFlowDefinition current = authnFlowDB.get(authFlowdef.getName());
		authFlowdef.setRevision(current.getRevision() + 1);	
		authnFlowDB.update(authFlowdef);	
	}
	
	private void assertIfAuthenticatorsExists(Set<String> toCheck, String flowName)
			throws EngineException
	{
		Set<String> existing = authenticatorDB.getAllNames();
		SetView<String> difference = Sets.difference(toCheck, existing);
		if (!difference.isEmpty())
			throw new IllegalArgumentException(
					"Can not add authentication flow " + flowName
					+ ", containing undefined authenticator(s) " + difference);
	}
}
