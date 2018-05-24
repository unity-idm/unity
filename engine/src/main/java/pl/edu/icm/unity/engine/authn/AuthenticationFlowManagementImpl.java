/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.authn;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;

/**
 * Authentication flow management implementation.
 * @author P.Piernik
 *
 */

@Component
@Primary
@InvocationEventProducer
@Transactional
public class AuthenticationFlowManagementImpl implements AuthenticationFlowManagement
{

	private AuthenticationFlowDB authnFlowDB;
	private AuthorizationManager authz;
	private AuthenticatorInstanceDB authenticatorDB;
	
	@Autowired
	public AuthenticationFlowManagementImpl(AuthenticationFlowDB authnFlowDB,
			AuthorizationManager authz, AuthenticatorInstanceDB authenticatorDB)
	{
		
		this.authnFlowDB = authnFlowDB;
		this.authz = authz;
		this.authenticatorDB = authenticatorDB;
	}

	
	@Override
	public void addAuthenticationFlowDefinition(
			AuthenticationFlowDefinition authFlowdef) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		
		checkIfAuthenticatorsExists(
				authFlowdef.getFirstFactorAuthenticators(),
				authFlowdef.getName());
		checkIfAuthenticatorsExists(
				authFlowdef.getSecondFactorAuthenticators(),
				authFlowdef.getName());
		authnFlowDB.create(authFlowdef);	
	}

	@Override
	public void removeAuthenticationFlowDefinition(String toRemove) throws EngineException
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
	public void updateAuthenticationFlowDefinition(AuthenticationFlowDefinition authFlowdef) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		checkIfAuthenticatorsExists(
				authFlowdef.getFirstFactorAuthenticators(),
				authFlowdef.getName());
		checkIfAuthenticatorsExists(
				authFlowdef.getSecondFactorAuthenticators(),
				authFlowdef.getName());
		authnFlowDB.update(authFlowdef);	
	}
	
	private void checkIfAuthenticatorsExists(Collection<String> toCheck, String flowName)
			throws EngineException
	{
		List<AuthenticatorInstance> all = authenticatorDB.getAll();
		List<String> allIds = all.stream().map(a -> a.getId()).collect(Collectors.toList());
		for (String toCheckId : toCheck)
		{
			if (!allIds.contains(toCheckId))
			{
				throw new IllegalArgumentException(
						"Can not add authentication flow " + flowName
								+ ", authenticator " + toCheckId
								+ " is undefined");
			}
		}

	}

}
