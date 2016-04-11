/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.identity;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hazelcast.core.HazelcastInstance;

import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Hazelcast store of {@link IdentityType}s.
 * 
 * @author K. Benedyczak
 */
@Repository
public class IdentityTypeHzStore implements IdentityTypeDAO
{
	private static final String STORE_ID = "identityTypesMap";
	
	private Map<String, IdentityType> hIdentityTypeMap;
	
	@Autowired
	public IdentityTypeHzStore(HazelcastInstance hazelcast)
	{
		hIdentityTypeMap = hazelcast.getMap(STORE_ID);
	}

	@Override
	public Map<String, IdentityType> getIdentityTypes()
	{
		return new HashMap<>(hIdentityTypeMap);
	}

	@Override
	public void updateIdentityType(IdentityType idType)
	{
		String key = getKey(idType);
		if (!hIdentityTypeMap.containsKey(key))
			throw new IllegalArgumentException("Identity type " + key + " does not exists");
		hIdentityTypeMap.put(key, idType);
	}

	@Override
	public void createIdentityType(IdentityType idType) throws IllegalArgumentException
	{
		if (idType.getDescription() == null)
			idType.setDescription(idType.getIdentityTypeProvider().getDefaultDescription());
		String key = getKey(idType);
		if (hIdentityTypeMap.containsKey(key))
			throw new IllegalArgumentException("Identity type " + key + " already exists");
		hIdentityTypeMap.put(key, idType);
	}

	@Override
	public void deleteIdentityType(String idType)
	{
		hIdentityTypeMap.remove(idType);
	}
	
	private String getKey(IdentityType idType)
	{
		return idType.getIdentityTypeProvider().getId();
	}

	@Override
	public IdentityType getIdentityType(String idType)
	{
		IdentityType identityType = hIdentityTypeMap.get(idType);
		if (identityType == null)
			throw new IllegalArgumentException("Identity type " + idType + " does not exists");
		return identityType;
	}
}
