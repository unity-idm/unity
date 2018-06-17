/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.Authenticator;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorsRegistry;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.credential.CredentialHolder;
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
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
	
	public List<Authenticator> getAuthenticators(Collection<String> ids)
	{
		List<Authenticator> authneticators = new ArrayList<>();
		for (String id : ids)
		{
			AuthenticatorImpl ret = getAuthenticator(id);
			authneticators.add(ret);
		}
		
		return authneticators; 
	}
	
	public AuthenticatorImpl getAuthenticator(String id) 
	{
		AuthenticatorInstance authnInstance = authenticatorDB.get(id);
		AuthenticatorImpl ret = getAuthenticatorNoCheck(authnInstance);
		return ret;
	}
	
	public AuthenticatorImpl getAuthenticatorNoCheck(AuthenticatorInstance authnInstance)
	{
		String localCredential = authnInstance.getLocalCredentialName();

		if (localCredential != null)
		{
			CredentialDefinition credDef = credRepository.get(localCredential);
			CredentialHolder credential = new CredentialHolder(credDef, localCredReg);
			String localCredentialConfig = credential.getCredentialDefinition()
					.getConfiguration();
			return new AuthenticatorImpl(identityResolver, authReg,
					authnInstance.getId(), authnInstance,
					localCredentialConfig);
		} else
			return new AuthenticatorImpl(identityResolver, authReg,
					authnInstance.getId(), authnInstance);
	}
	
	public List<AuthenticationFlow> resolveAndGetAuthenticationFlows(List<String> authnOptions)
	{
		Map<String, AuthenticationFlowDefinition> allFlows = authenticationFlowDB
				.getAllAsMap();
		Map<String, AuthenticatorInstance> allAuthenticators = authenticatorDB
				.getAllAsMap();

		List<AuthenticationFlowDefinition> defs = new ArrayList<>();

		for (String authOption : authnOptions)
		{
			AuthenticationFlowDefinition def = allFlows.get(authOption);
			if (def == null)
			{
				AuthenticatorInstance authenticator = allAuthenticators
						.get(authOption);
				if (authenticator != null)
				{
					def = new AuthenticationFlowDefinition(
							authenticator.getId(), Policy.NEVER,
							Sets.newHashSet(authenticator.getId()));
				} else
				{
					throw new IllegalArgumentException(
							"Authentication flow or authenticator "
									+ authOption
									+ " is undefined");
				}
			}

			defs.add(def);
		}
		return getAuthenticationFlows(defs);

	}
	
	public List<AuthenticationFlow> getAuthenticationFlows(
			List<AuthenticationFlowDefinition> authnFlows)
	{
		List<AuthenticationFlow> ret = new ArrayList<>(authnFlows.size());

		for (AuthenticationFlowDefinition authenticationFlowDefinition : authnFlows)
		{
			
			List<Authenticator> firstFactorAuthImpl = getAuthenticators(
					authenticationFlowDefinition
							.getFirstFactorAuthenticators());
			List<Authenticator> secondFactorFactorAuthImpl = getAuthenticators(
					authenticationFlowDefinition
							.getSecondFactorAuthenticators());
			
			ret.add(new AuthenticationFlow(authenticationFlowDefinition.getName(),
					authenticationFlowDefinition.getPolicy(),
					Sets.newHashSet(firstFactorAuthImpl),
					secondFactorFactorAuthImpl, authenticationFlowDefinition.getRevision()));
		}
		return ret;
	}
}
