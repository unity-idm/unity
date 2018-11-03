/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.local;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialType;

/**
 * Registry of components which are used to create {@link LocalCredentialVerificator}s.
 * 
 * @author K. Benedyczak
 */
@Component
public class LocalCredentialsRegistry
{
	private Map<String, LocalCredentialVerificatorFactory> localCredentialVerificatorFactories;
	private Cache<CredentialDefinition, LocalCredentialVerificator> verificatorsCache = CacheBuilder.newBuilder()
			.expireAfterAccess(Duration.ofMinutes(30))
			.build();
	
	@Autowired
	public LocalCredentialsRegistry(Optional<List<LocalCredentialVerificatorFactory>> verificatorFactories)
	{
		localCredentialVerificatorFactories = new HashMap<String, LocalCredentialVerificatorFactory>();
		
		for (LocalCredentialVerificatorFactory f: verificatorFactories.orElseGet(ArrayList::new))
			localCredentialVerificatorFactories.put(f.getName(), (LocalCredentialVerificatorFactory) f);	
	}

	public Set<CredentialType> getLocalCredentialTypes()
	{
		Set<CredentialType> ret = new HashSet<CredentialType>();
		
		for (LocalCredentialVerificatorFactory fact: localCredentialVerificatorFactories.values())
		{
			CredentialType credentialType = new CredentialType(fact.getName(), fact.getDescription(),
					fact.isSupportingInvalidation());
			ret.add(credentialType);
		}
		return ret;
	}
	
	public LocalCredentialVerificatorFactory getLocalCredentialFactory(String id)
	{
		return localCredentialVerificatorFactories.get(id);
	}
	
	public LocalCredentialVerificator createLocalCredentialVerificator(CredentialDefinition def) 
			throws IllegalCredentialException
	{
		LocalCredentialVerificator cached = verificatorsCache.getIfPresent(def);
		if (cached == null)
		{
			cached = createLocalCredentialVerificatorNoCache(def);
			verificatorsCache.put(def, cached);
		}
		return cached;
	}

	private LocalCredentialVerificator createLocalCredentialVerificatorNoCache(CredentialDefinition def) 
			throws IllegalCredentialException
	{
		LocalCredentialVerificatorFactory fact = getLocalCredentialFactory(def.getTypeId());
		if (fact == null)
			throw new IllegalCredentialException("The credential type " + def.getTypeId() + " is unknown");
		LocalCredentialVerificator validator = fact.newInstance();
		validator.setSerializedConfiguration(def.getConfiguration());
		validator.setCredentialName(def.getName());
		return validator;
	}
}
