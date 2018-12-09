/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator.VerificatorType;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;

/**
 * See {@link AuthenticatorSupportService}
 */
@Primary
@Component
public class AuthenticatorSupportServiceImpl implements AuthenticatorSupportService
{
	private AuthenticatorLoader authnLoader;
	private AuthenticatorsRegistry authnRegistry;
	private AuthenticatorManagement authenticationManagement;
	
	@Autowired
	public AuthenticatorSupportServiceImpl(AuthenticatorLoader authnLoader, 
			AuthenticatorsRegistry authnRegistry, AuthenticatorManagement authenticationManagement)
	{
		this.authnLoader = authnLoader;
		this.authnRegistry = authnRegistry;
		this.authenticationManagement = authenticationManagement;
	}


	@Override
	@Transactional
	public List<AuthenticationFlow> getRemoteAuthenticatorsAsFlows(String bindingId) throws EngineException
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
		return authnLoader.createAuthenticationFlows(flows, bindingId);
	}

	@Override
	@Transactional
	public List<AuthenticationFlow> resolveAuthenticationFlows(List<String> authnOptions, String bindingId)
	{
		return authnLoader.resolveAuthenticationFlows(authnOptions, bindingId);
	}


	@Override
	@Transactional
	public List<AuthenticatorInstance> getRemoteAuthenticators(String bindingId) throws EngineException
	{
		ArrayList<AuthenticatorInstance> ret = new ArrayList<>();

		Collection<AuthenticatorInstance> authnInstances = authnLoader.getAuthenticators(bindingId);
		for (AuthenticatorInstance authenticator : authnInstances)
		{
			if (authenticator.getMetadata().getLocalCredentialName() == null)
			{
				ret.add(authnLoader.getAuthenticator(authenticator.getMetadata().getName(), 
						bindingId));
			}
		}
		return ret;
	}
}
