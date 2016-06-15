/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorsRegistry;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.credential.CredentialHolder;
import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
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
			CredentialDefinition credDef = credDB.get(localCredential);
			CredentialHolder credential = new CredentialHolder(credDef, localCredReg);
			ObjectNode localCredentialConfig = credential.getCredentialDefinition().
					getJsonConfiguration();
			return new AuthenticatorImpl(identityResolver, authReg,	authnInstance.getId(), 
					authnInstance, JsonUtil.serialize(localCredentialConfig));
		} else
			return new AuthenticatorImpl(identityResolver, authReg, 
				authnInstance.getId(), authnInstance);
	}
	
	public List<AuthenticationOption> getAuthenticators(List<AuthenticationOptionDescription> authn) 
	{
		List<AuthenticationOption> ret = new ArrayList<>(authn.size());
		for (AuthenticationOptionDescription aSet: authn)
		{
			CredentialRetrieval primaryAuthImpl = getAuthenticator(aSet.getPrimaryAuthenticator()).
					getRetrieval();
			CredentialRetrieval secondaryAuthImpl = aSet.getMandatory2ndAuthenticator() == null ?
					null : getAuthenticator(aSet.getMandatory2ndAuthenticator()).getRetrieval();
			ret.add(new AuthenticationOption(primaryAuthImpl, secondaryAuthImpl));
		}
		return ret;
	}
}
