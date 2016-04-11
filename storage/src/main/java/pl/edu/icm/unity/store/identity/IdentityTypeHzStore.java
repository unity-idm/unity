/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.identity;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.hazelcast.core.TransactionalMap;

import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.tx.TransactionTL;
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
	
	@Override
	public Map<String, IdentityType> getIdentityTypes()
	{
		TransactionalMap<String, IdentityType> hIdentityTypeMap = getMap();
		Map<String, IdentityType> ret = new HashMap<>();
		for (String key: hIdentityTypeMap.keySet())
			ret.put(key, hIdentityTypeMap.get(key));
		return ret;
	}

	@Override
	public void updateIdentityType(IdentityType idType)
	{
		TransactionalMap<String, IdentityType> hIdentityTypeMap = getMap();
		String key = getKey(idType);
		if (!hIdentityTypeMap.containsKey(key))
			throw new IllegalArgumentException("Identity type " + key + " does not exists");
		hIdentityTypeMap.put(key, idType);
	}

	@Override
	public void createIdentityType(IdentityType idType) throws IllegalArgumentException
	{
		TransactionalMap<String, IdentityType> hIdentityTypeMap = getMap();
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
		getMap().remove(idType);
	}

	@Override
	public IdentityType getIdentityType(String idType)
	{
		IdentityType identityType = getMap().get(idType);
		if (identityType == null)
			throw new IllegalArgumentException("Identity type " + idType + " does not exists");
		return identityType;
	}
	
	private String getKey(IdentityType idType)
	{
		return idType.getIdentityTypeProvider().getId();
	}
	
	private TransactionalMap<String, IdentityType> getMap()
	{
		return TransactionTL.getHzContext().getMap(STORE_ID);
	}
}
