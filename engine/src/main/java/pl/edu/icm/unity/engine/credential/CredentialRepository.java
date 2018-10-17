/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

/**
 * Allows read credentials from DB and @{SystemCredentialProvider}
 * @author P.Piernik
 *
 */
@Component
public class CredentialRepository
{
	private CredentialDB credentialDB;
	private SystemCredentialProvider sysProvider;
	
	@Autowired
	public CredentialRepository(CredentialDB credentialDB, SystemCredentialProvider sysProvider)
	{
		this.credentialDB = credentialDB;
		this.sysProvider = sysProvider;
	}
	
	@Transactional
	public Collection<CredentialDefinition> getCredentialDefinitions() throws EngineException
	{
		List<CredentialDefinition> res =  new ArrayList<>();
		res.addAll(sysProvider.getSystemCredentials());
		res.addAll(credentialDB.getAll());
		return res;
	}
	
	@Transactional
	public CredentialDefinition get(String credentialName)
	{
		for (CredentialDefinition cr : sysProvider.getSystemCredentials())
			if (cr.getName().equals(credentialName))
				return cr;
		return credentialDB.get(credentialName);
	}

	public void assertExist(Set<String> names) throws EngineException
	{	
		Set<String> allNames = getAllNames();
		Set<String> missing = Sets.difference(new HashSet<>(names), allNames);
		if (missing.isEmpty())
			return;
		
		throw new IllegalArgumentException("The following credentials are not available: " 
				+ missing.toString());		
		
	}

	private Set<String> getAllNames() throws EngineException
	{
		return getCredentialDefinitions().stream().map(c -> c.getName()).collect(Collectors.toSet());
	}

}
