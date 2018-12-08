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
import pl.edu.icm.unity.engine.api.authn.Authenticator;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorsRegistry;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.credential.CredentialHolder;
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
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
	private IdentityResolver identityResolver;
	private AuthenticatorInstanceDB authenticatorDB;
	private AuthenticationFlowDB authenticationFlowDB;
	private AuthenticatorsRegistry authReg;
	private LocalCredentialsRegistry localCredReg;
	private CredentialRepository credRepository;
	
	@Autowired
	public AuthenticatorLoader(IdentityResolver identityResolver,
			AuthenticatorInstanceDB authenticatorDB,
			AuthenticationFlowDB authenticationFlowDB, AuthenticatorsRegistry authReg,
			CredentialRepository credRepository, LocalCredentialsRegistry localCredReg)
	{
		this.localCredReg = localCredReg;
		this.identityResolver = identityResolver;
		this.authenticatorDB = authenticatorDB;
		this.authReg = authReg;
		this.credRepository = credRepository;
		this.authenticationFlowDB = authenticationFlowDB;
	}
	
	public List<AuthenticationFlow> resolveAndGetAuthenticationFlows(List<String> authnOptions, String binding)
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
		return getAuthenticationFlows(defs, binding);

	}
	
	List<AuthenticationFlow> getAuthenticationFlows(List<AuthenticationFlowDefinition> authnFlows, String binding)
	{
		List<AuthenticationFlow> ret = new ArrayList<>(authnFlows.size());

		for (AuthenticationFlowDefinition authenticationFlowDefinition : authnFlows)
		{
			List<Authenticator> firstFactorAuthImpl = getAuthenticators(
					authenticationFlowDefinition.getFirstFactorAuthenticators(), binding);
			List<Authenticator> secondFactorFactorAuthImpl = getAuthenticators(
					authenticationFlowDefinition.getSecondFactorAuthenticators(), binding);
			
			ret.add(new AuthenticationFlow(authenticationFlowDefinition.getName(),
					authenticationFlowDefinition.getPolicy(),
					Sets.newHashSet(firstFactorAuthImpl),
					secondFactorFactorAuthImpl, authenticationFlowDefinition.getRevision()));
		}
		return ret;
	}
	
	AuthenticatorImpl getAuthenticator(String id, String binding) 
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
	
	private AuthenticatorImpl getAuthenticatorNoCheck(AuthenticatorConfiguration authnConfiguration, String binding)
	{
		String localCredential = authnConfiguration.getLocalCredentialName();

		if (localCredential != null)
		{
			CredentialDefinition credDef = credRepository.get(localCredential);
			CredentialHolder credential = new CredentialHolder(credDef, localCredReg);
			String localCredentialConfig = credential.getCredentialDefinition()
					.getConfiguration();
			return new AuthenticatorImpl(identityResolver, authReg,
					authnConfiguration.getName(), authnConfiguration,
					localCredentialConfig, binding);
		} else
			return new AuthenticatorImpl(identityResolver, authReg,
					authnConfiguration.getName(), authnConfiguration, binding);
	}
	
	
	private List<Authenticator> getAuthenticators(Collection<String> ids, String binding)
	{
		return ids.stream()
				.map(id -> getAuthenticator(id, binding))
				.collect(Collectors.toList());
	}
}
