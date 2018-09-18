/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorsRegistry;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator.VerificatorType;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;

/**
 * Implementation of {@link AuthenticatorsManagement}
 * 
 * @author K. Benedyczak
 */
@Component
public class AuthenticatorsManagementImpl implements AuthenticatorSupportManagement
{
	private AuthenticatorLoader authnLoader;
	private AuthenticatorInstanceDB authenticatorDB;
	private AuthenticatorsRegistry authnRegistry;
	private AuthenticatorManagement authenticationManagement;
	
	@Autowired
	public AuthenticatorsManagementImpl(AuthenticatorLoader authnLoader, AuthenticatorInstanceDB authenticatorDB,
			AuthenticatorsRegistry authnRegistry, AuthenticatorManagement authenticationManagement)
	{
		this.authnLoader = authnLoader;
		this.authenticatorDB = authenticatorDB;
		this.authnRegistry = authnRegistry;
		this.authenticationManagement = authenticationManagement;
	}


	@Override
	@Transactional
	public List<AuthenticationFlow> getAuthenticatorUIs(List<AuthenticationFlowDefinition> authnFlows)
			throws EngineException
	{
		return authnLoader.getAuthenticationFlows(authnFlows);
	}
	
	@Override
	@Transactional
	public void removeAllPersistedAuthenticators() throws EngineException
	{
		authenticatorDB.deleteAll();
	}
	
	@Override
	@Transactional
	public List<AuthenticationFlowDefinition> resolveAllRemoteAuthenticatorFlows(String bindingId)
			throws EngineException
	{
		ArrayList<AuthenticationFlowDefinition> flows = new ArrayList<>();

		Collection<AuthenticatorInstance> authnInstances = authenticationManagement.getAuthenticators(bindingId);
		for (AuthenticatorInstance instance : authnInstances)
		{
			CredentialVerificatorFactory factory = authnRegistry
					.getCredentialVerificatorFactory(instance.getTypeDescription().getVerificationMethod());
			CredentialVerificator verificator = factory.newInstance();
			if (verificator.getType().equals(VerificatorType.Remote))
			{
				AuthenticationFlowDefinition authnFlow = new AuthenticationFlowDefinition(instance.getId(),
						Policy.NEVER, Sets.newHashSet(instance.getId()));
				flows.add(authnFlow);
			}
		}

		return flows;
	}

	@Override
	@Transactional
	public List<AuthenticationFlow> resolveAndGetAuthenticationFlows(List<String> authnOptions)
	{
		return authnLoader.resolveAndGetAuthenticationFlows(authnOptions);
	}
}
