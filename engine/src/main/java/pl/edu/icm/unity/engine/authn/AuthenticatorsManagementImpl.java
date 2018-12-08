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
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;

/**
 * See {@link AuthenticatorSupportManagement}
 * FIXME rename class, methods
 */
@Component
public class AuthenticatorsManagementImpl implements AuthenticatorSupportManagement
{
	private AuthenticatorLoader authnLoader;
	private AuthenticatorsRegistry authnRegistry;
	private AuthenticatorManagement authenticationManagement;
	
	@Autowired
	public AuthenticatorsManagementImpl(AuthenticatorLoader authnLoader, 
			AuthenticatorsRegistry authnRegistry, AuthenticatorManagement authenticationManagement)
	{
		this.authnLoader = authnLoader;
		this.authnRegistry = authnRegistry;
		this.authenticationManagement = authenticationManagement;
	}


	@Override
	@Transactional
	public List<AuthenticationFlow> getAuthenticatorUIs(List<AuthenticationFlowDefinition> authnFlows, String bindingId)
			throws EngineException
	{
		return authnLoader.getAuthenticationFlows(authnFlows, bindingId);
	}
	
	@Override
	@Transactional
	public List<AuthenticationFlowDefinition> resolveAllRemoteAuthenticatorFlows(String bindingId)
			throws EngineException
	{
		ArrayList<AuthenticationFlowDefinition> flows = new ArrayList<>();

		Collection<AuthenticatorInfo> authnInstances = authenticationManagement.getAuthenticators(bindingId);
		for (AuthenticatorInfo authenticator : authnInstances)
		{
			CredentialVerificatorFactory factory = authnRegistry
					.getCredentialVerificatorFactory(authenticator.getTypeDescription().getVerificationMethod());
			CredentialVerificator verificator = factory.newInstance();
			if (verificator.getType().equals(VerificatorType.Remote))
			{
				AuthenticationFlowDefinition authnFlow = new AuthenticationFlowDefinition(authenticator.getId(),
						Policy.NEVER, Sets.newHashSet(authenticator.getId()));
				flows.add(authnFlow);
			}
		}

		return flows;
	}

	@Override
	@Transactional
	public List<AuthenticationFlow> resolveAndGetAuthenticationFlows(List<String> authnOptions, String bindingId)
	{
		return authnLoader.resolveAndGetAuthenticationFlows(authnOptions, bindingId);
	}
}
