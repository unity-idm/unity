/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.authn.AuthenticatorInstanceDB;
import pl.edu.icm.unity.db.generic.cred.CredentialDB;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.server.registries.LocalCredentialsRegistry;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
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
	private AuthenticatorsRegistry authReg;
	private LocalCredentialsRegistry localCredReg;
	private CredentialDB credDB;
	
	@Autowired
	public AuthenticatorLoader(IdentityResolver identityResolver,
			AuthenticatorInstanceDB authenticatorDB, AuthenticatorsRegistry authReg,
			CredentialDB credDB, LocalCredentialsRegistry localCredReg)
	{
		this.localCredReg = localCredReg;
		this.identityResolver = identityResolver;
		this.authenticatorDB = authenticatorDB;
		this.authReg = authReg;
		this.credDB = credDB;
	}

	public AuthenticatorImpl getAuthenticator(String id, SqlSession sql) 
			throws EngineException
	{
		AuthenticatorInstance authnInstance = authenticatorDB.get(id, sql);
		AuthenticatorImpl ret = getAuthenticatorNoCheck(authnInstance, sql);
		return ret;
	}

	public AuthenticatorImpl getAuthenticatorNoCheck(AuthenticatorInstance authnInstance, SqlSession sql) 
			throws EngineException
	{
		String localCredential = authnInstance.getLocalCredentialName();
		
		if (localCredential != null)
		{
			CredentialDefinition credDef = credDB.get(localCredential, sql);
			CredentialHolder credential = new CredentialHolder(credDef, localCredReg);
			String localCredentialConfig = credential.getCredentialDefinition().
					getJsonConfiguration();
			return new AuthenticatorImpl(identityResolver, authReg,	authnInstance.getId(), 
					authnInstance, localCredentialConfig);
		} else
			return new AuthenticatorImpl(identityResolver, authReg, 
				authnInstance.getId(), authnInstance);
	}
	
	public List<AuthenticationOption> getAuthenticators(List<AuthenticationOptionDescription> authn, SqlSession sql) 
			throws EngineException
	{
		List<AuthenticationOption> ret = new ArrayList<>(authn.size());
		for (AuthenticationOptionDescription aSet: authn)
		{
			Set<String> authenticators = aSet.getAuthenticators();
			Map<String, BindingAuthn> aImpls = new HashMap<String, BindingAuthn>();
			for (String authenticator: authenticators)
			{
				AuthenticatorImpl authImpl = getAuthenticator(authenticator, sql);
				aImpls.put(authenticator, authImpl.getRetrieval());
			}
			ret.add(new AuthenticationOption(aImpls));
		}
		return ret;
	}
}
