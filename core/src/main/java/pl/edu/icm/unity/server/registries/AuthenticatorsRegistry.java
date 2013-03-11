/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.server.authn.CredentialVerificator;
import pl.edu.icm.unity.server.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.server.authn.LocalCredentialHandlerFactory;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificatorFactory;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.CredentialType;

/**
 * Registry of components which are used to create {@link AuthenticatorInstance}s and local credential handlers.
 * 
 * @author K. Benedyczak
 */
@Component
public class AuthenticatorsRegistry
{
	private Map<String, CredentialRetrievalFactory> credentialRetrievalFactories;
	private Map<String, CredentialVerificatorFactory> credentialVerificatorFactories;
	
	private Map<String, Set<AuthenticatorTypeDescription>> authenticatorsByBinding;
	private Map<String, AuthenticatorTypeDescription> authenticatorsById;
	
	private Map<String, LocalCredentialHandlerFactory> localCredentialHandlerFactories;
	
	@Autowired
	public AuthenticatorsRegistry(List<CredentialRetrievalFactory> retrievalFactories, 
			List<CredentialVerificatorFactory> verificatorFactories,
			List<LocalCredentialHandlerFactory> localCredentialFactories)
	{
		authenticatorsByBinding = new HashMap<String, Set<AuthenticatorTypeDescription>>();
		authenticatorsById = new HashMap<String, AuthenticatorTypeDescription>();
		
		credentialRetrievalFactories = new HashMap<String, CredentialRetrievalFactory>();
		credentialVerificatorFactories = new HashMap<String, CredentialVerificatorFactory>();
		localCredentialHandlerFactories = new HashMap<String, LocalCredentialHandlerFactory>();
		
		for (CredentialRetrievalFactory f: retrievalFactories)
			credentialRetrievalFactories.put(f.getName(), f);
		for (CredentialVerificatorFactory f: verificatorFactories)
			credentialVerificatorFactories.put(f.getName(), f);

		for (LocalCredentialHandlerFactory f: localCredentialFactories)
			localCredentialHandlerFactories.put(f.getName(), f);
		
		
		for (int j=0; j<verificatorFactories.size(); j++)
		{
			CredentialVerificatorFactory vf = verificatorFactories.get(j);
			CredentialVerificator verificator = vf.newInstance();
			for (int i=j; i<retrievalFactories.size(); i++)
			{
				CredentialRetrievalFactory rf = retrievalFactories.get(i);
				if (!rf.isCredentialExchangeSupported(verificator))
					continue;
				AuthenticatorTypeDescription desc = new AuthenticatorTypeDescription();
				desc.setId(vf.getName() + " with " + rf.getName());
				desc.setRetrievalMethod(rf.getName());
				desc.setRetrievalMethodDescription(rf.getDescription());
				desc.setSupportedBinding(rf.getSupportedBinding());
				desc.setVerificationMethod(vf.getName());
				desc.setVerificationMethodDescription(vf.getDescription());
				desc.setLocal(vf instanceof LocalCredentialVerificatorFactory);
				Set<AuthenticatorTypeDescription> existing = authenticatorsByBinding.get(
						rf.getSupportedBinding());
				if (existing == null)
				{
					existing = new HashSet<AuthenticatorTypeDescription>();
					authenticatorsByBinding.put(rf.getSupportedBinding(), existing);
				}
				existing.add(desc);
				authenticatorsById.put(desc.getId(), desc);
			}
		}
		
		authenticatorsByBinding = Collections.unmodifiableMap(authenticatorsByBinding);
	}

	public CredentialRetrievalFactory getCredentialRetrievalFactory(String id)
	{
		return credentialRetrievalFactories.get(id);
	}

	public CredentialVerificatorFactory getCredentialVerificatorFactory(String id)
	{
		return credentialVerificatorFactories.get(id);
	}

	public AuthenticatorTypeDescription getAuthenticatorsById(String id)
	{
		return authenticatorsById.get(id);
	}

	public Set<AuthenticatorTypeDescription> getAuthenticatorsByBinding(String binding)
	{
		return authenticatorsByBinding.get(binding);
	}
	
	public Set<AuthenticatorTypeDescription> getAuthenticators()
	{
		Set<AuthenticatorTypeDescription> ret = new HashSet<AuthenticatorTypeDescription>();
		for (String binding: authenticatorsByBinding.keySet())
			ret.addAll(authenticatorsByBinding.get(binding));
		return ret;
	}
	
	public Set<CredentialType> getLocalCredentialTypes()
	{
		Set<CredentialType> ret = new HashSet<CredentialType>();
		
		for (LocalCredentialHandlerFactory fact: localCredentialHandlerFactories.values())
			ret.add(fact.getCredentialType());
		
		return ret;
	}
	
	public LocalCredentialHandlerFactory getLocalCredentialFactory(String id)
	{
		return localCredentialHandlerFactories.get(id);
	}
}
