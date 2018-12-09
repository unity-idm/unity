/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator.VerificatorType;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;

/**
 * Registry of components which are used to create authenticators and local credential handlers.
 * 
 * @author K. Benedyczak
 */
@Component
public class AuthenticatorsRegistry
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, AuthenticatorsRegistry.class);
	
	private Map<String, CredentialRetrievalFactory> credentialRetrievalFactories;
	private Map<String, CredentialVerificatorFactory> credentialVerificatorFactories;
	
	private Map<String, Set<AuthenticatorTypeDescription>> authenticatorsByBinding;
	private Map<String, AuthenticatorTypeDescription> authenticatorsById;
	
	@Autowired
	public AuthenticatorsRegistry(Optional<List<CredentialRetrievalFactory>> retrievalFactoriesO, 
			Optional<List<CredentialVerificatorFactory>> verificatorFactoriesO)
	{
		List<CredentialRetrievalFactory> retrievalFactories = retrievalFactoriesO.orElseGet(ArrayList::new);
		List<CredentialVerificatorFactory> verificatorFactories = verificatorFactoriesO.orElseGet(ArrayList::new);
		
		authenticatorsByBinding = new HashMap<>();
		authenticatorsById = new HashMap<>();
		
		credentialRetrievalFactories = new HashMap<>();
		credentialVerificatorFactories = new HashMap<>();
		
		for (CredentialRetrievalFactory f: retrievalFactories)
			credentialRetrievalFactories.put(f.getName(), f);
		for (CredentialVerificatorFactory f: verificatorFactories)
			credentialVerificatorFactories.put(f.getName(), f);
		
		log.debug("The following authenticator types are available:");
		for (int j=0; j<verificatorFactories.size(); j++)
		{
			CredentialVerificatorFactory vf = verificatorFactories.get(j);
			CredentialVerificator verificator = vf.newInstance();
			for (int i=0; i<retrievalFactories.size(); i++)
			{
				CredentialRetrievalFactory rf = retrievalFactories.get(i);
				if (!rf.isCredentialExchangeSupported(verificator))
					continue;
				AuthenticatorTypeDescription desc = new AuthenticatorTypeDescription(
						vf.getName(),
						vf.getDescription(),
						verificator.getType() == VerificatorType.Local);
				Set<AuthenticatorTypeDescription> byBinding = authenticatorsByBinding.get(
						rf.getSupportedBinding());
				if (byBinding == null)
				{
					byBinding = new HashSet<>();
					authenticatorsByBinding.put(rf.getSupportedBinding(), byBinding);
				}
				byBinding.add(desc);
				authenticatorsById.put(desc.getVerificationMethod(), desc);
			}
			log.debug(" - >" + vf.getName() + "< supporting " + getSupportedBindings(vf.getName()));
		}
		
		authenticatorsByBinding = Collections.unmodifiableMap(authenticatorsByBinding);
	}

	public CredentialRetrievalFactory findCredentialRetrieval(String binding, CredentialExchange credExchange)
	{
		for (CredentialRetrievalFactory retrieval: credentialRetrievalFactories.values())
		{
			if (binding.equals(retrieval.getSupportedBinding()) && 
					retrieval.isCredentialExchangeSupported(credExchange))
				return retrieval;
		}
		throw new IllegalArgumentException("There is no credential retrieval for binding " + binding + 
				" and credential type " + credExchange.getExchangeId());
	}
	
	public Set<String> getSupportedBindings(String verificatorId)
	{
		return getSupportedRetrievals(verificatorId).stream()
				.map(retrieval -> retrieval.getSupportedBinding())
				.collect(Collectors.toSet());
	}	

	public Set<CredentialRetrievalFactory> getSupportedRetrievals(String verificatorId)
	{
		CredentialVerificatorFactory verificatorFactory = credentialVerificatorFactories.get(verificatorId);
		if (verificatorFactory == null)
			throw new IllegalArgumentException("Unknown verificator: " + verificatorId);
		CredentialVerificator verificator = verificatorFactory.newInstance();
		Set<CredentialRetrievalFactory> supported = new HashSet<>();
		for (CredentialRetrievalFactory retrieval: credentialRetrievalFactories.values())
		{
			if (retrieval.isCredentialExchangeSupported(verificator))
				supported.add(retrieval);
		}
		return supported;
	}

	
	public CredentialRetrievalFactory getCredentialRetrievalFactory(String id)
	{
		return credentialRetrievalFactories.get(id);
	}

	public CredentialVerificatorFactory getCredentialVerificatorFactory(String id)
	{
		return credentialVerificatorFactories.get(id);
	}

	public AuthenticatorTypeDescription getAuthenticatorTypeById(String id)
	{
		return authenticatorsById.get(id);
	}

	public Set<AuthenticatorTypeDescription> getAuthenticatorTypesByBinding(String binding)
	{
		return authenticatorsByBinding.get(binding);
	}
	
	public Set<AuthenticatorTypeDescription> getAuthenticatorTypes()
	{
		Set<AuthenticatorTypeDescription> ret = new HashSet<AuthenticatorTypeDescription>();
		for (Map.Entry<String, Set<AuthenticatorTypeDescription>> entry: authenticatorsByBinding.entrySet())
			ret.addAll(entry.getValue());
		return ret;
	}
	
	public Set<String> getAuthenticatorTypeNames()
	{
		return new HashSet<String>(authenticatorsById.keySet());
	}
	
}
