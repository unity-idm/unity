/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.credential.CredentialHolder;
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorConfigurationDB;
import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

/**
 * Loading and initialization of {@link AuthenticatorImpl}.
 * 
 * @author K. Benedyczak
 */
@Component
public class AuthenticatorLoader
{
	private AuthenticatorConfigurationDB authenticatorDB;
	private AuthenticationFlowDB authenticationFlowDB;
	private AuthenticatorsRegistry authReg;
	private LocalCredentialsRegistry localCredReg;
	private CredentialRepository credRepository;
	private AuthenticatorFactory authenticatorFactory;
	
	@Autowired
	public AuthenticatorLoader(AuthenticatorConfigurationDB authenticatorDB,
			AuthenticationFlowDB authenticationFlowDB, AuthenticatorsRegistry authReg,
			CredentialRepository credRepository, LocalCredentialsRegistry localCredReg,
			AuthenticatorFactory authenticatorFactory)
	{
		this.localCredReg = localCredReg;
		this.authenticatorDB = authenticatorDB;
		this.authReg = authReg;
		this.credRepository = credRepository;
		this.authenticationFlowDB = authenticationFlowDB;
		this.authenticatorFactory = authenticatorFactory;
	}
	
	public List<AuthenticationFlow> resolveAuthenticationFlows(List<String> authnOptions, String binding)
	{
		Map<String, AuthenticationFlowDefinition> allFlows = authenticationFlowDB.getAllAsMap();
		Map<String, AuthenticatorConfiguration> allAuthenticators = authenticatorDB.getAllAsMap();

		List<AuthenticationFlowDefinition> defs = new ArrayList<>();

		for (String authOption : authnOptions)
		{
			AuthenticationFlowDefinition def = allFlows.get(authOption);
			if (def == null)
			{
				AuthenticatorConfiguration authenticator = allAuthenticators.get(authOption);
				def = createAdHocAuthenticatorWrappingFlow(authOption, authenticator);
			}
			defs.add(def);
		}
		return createAuthenticationFlows(defs, binding);

	}
	
	List<AuthenticationFlow> createAuthenticationFlows(List<AuthenticationFlowDefinition> authnFlows, String binding)
	{
		List<AuthenticationFlow> ret = new ArrayList<>(authnFlows.size());

		for (AuthenticationFlowDefinition authenticationFlowDefinition : authnFlows)
		{
			List<AuthenticatorInstance> firstFactorAuthImpl = getAuthenticators(
					authenticationFlowDefinition.getFirstFactorAuthenticators(), binding);
			List<AuthenticatorInstance> secondFactorFactorAuthImpl = getAuthenticators(
					authenticationFlowDefinition.getSecondFactorAuthenticators(), binding);
			
			ret.add(new AuthenticationFlow(authenticationFlowDefinition.getName(),
					authenticationFlowDefinition.getPolicy(),
					Sets.newHashSet(firstFactorAuthImpl),
					secondFactorFactorAuthImpl, authenticationFlowDefinition.getRevision()));
		}
		return ret;
	}
	
	AuthenticatorInstance getAuthenticator(String id, String binding) 
	{
		AuthenticatorConfiguration authnConfig = authenticatorDB.get(id);
		return getAuthenticatorNoCheck(authnConfig, binding);
	}
	
	/**
	 * Checks if configuration is valid for corresponding verificator and all available retrievals
	 */
	void verifyConfiguration(String typeId, String config)
	{
		CredentialVerificatorFactory verificatorFact = authReg.getCredentialVerificatorFactory(typeId);
		CredentialVerificator verificator = verificatorFact.newInstance();
		verificator.setSerializedConfiguration(config);
		
		Set<CredentialRetrievalFactory> supportedRetrievals = authReg.getSupportedRetrievals(typeId);
		for (CredentialRetrievalFactory retrievalFact: supportedRetrievals)
		{
			CredentialRetrieval newInstance = retrievalFact.newInstance();
			newInstance.setSerializedConfiguration(config);
		}
	}
	
	private AuthenticationFlowDefinition createAdHocAuthenticatorWrappingFlow(String authOption,
			AuthenticatorConfiguration authenticator)
	{
		if (authenticator != null)
		{
			return new AuthenticationFlowDefinition(
					authenticator.getName(), Policy.NEVER,
					Sets.newHashSet(authenticator.getName()));
		} else
		{
			throw new IllegalArgumentException(
					"Authentication flow or authenticator "
							+ authOption
							+ " is undefined");
		}
	}
	
	private AuthenticatorInstance getAuthenticatorNoCheck(AuthenticatorConfiguration authnConfiguration, String binding)
	{
		String localCredential = authnConfiguration.getLocalCredentialName();

		if (localCredential != null)
		{
			CredentialDefinition credDef = credRepository.get(localCredential);
			CredentialHolder credential = new CredentialHolder(credDef, localCredReg);
			String localCredentialConfig = credential.getCredentialDefinition()
					.getConfiguration();
			return authenticatorFactory.restoreLocalAuthenticator(
					authnConfiguration, localCredentialConfig, binding);
		} else
		{
			return authenticatorFactory.restoreRemoteAuthenticator(	authnConfiguration, binding);
		}
	}
	
	
	private List<AuthenticatorInstance> getAuthenticators(Collection<String> ids, String binding)
	{
		return ids.stream()
				.map(id -> getAuthenticator(id, binding))
				.collect(Collectors.toList());
	}
	
	List<AuthenticatorInstance> getAuthenticators(String binding)
	{
		return authenticatorDB.getAll().stream()
				.filter(ac -> authReg.getSupportedBindings(ac.getVerificationMethod()).contains(binding))
				.map(ac -> getAuthenticator(ac.getName(), binding))
				.collect(Collectors.toList());
	}
}
