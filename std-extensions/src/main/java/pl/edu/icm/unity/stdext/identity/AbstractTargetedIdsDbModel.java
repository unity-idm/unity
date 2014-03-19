/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utility class, used by targeted identities. Allows for storage and manipulation of targeted identities
 * with support for two level addressing (realm and target). The primary function is to serialize the structure 
 * to/from String. 
 * <p>
 * The functionality is modeled as a generic abstract class, so the actual contents of the targeted value
 * is defined by extension.  
 * @author K. Benedyczak
 */
public abstract class AbstractTargetedIdsDbModel<T>
{
	protected ObjectMapper mapper;
	
	/**
	 * Map indexed with realm names. Values are again maps,this time indexed with target name. Values of the
	 * inner map are the targeted identities.
	 */
	protected Map<String, Map<String, T>> realmsMap;
	
	public AbstractTargetedIdsDbModel(ObjectMapper mapper, String value)
	{
		this.mapper = mapper;
		realmsMap = new HashMap<String, Map<String, T>>();
		if (value == null)
			return;
		try
		{
			ObjectNode root = (ObjectNode) mapper.readTree(value);
			ObjectNode ids = (ObjectNode) root.get("mapOfIds");
			Iterator<Entry<String, JsonNode>> realms = ids.fields();
			while(realms.hasNext())
			{
				Entry<String, JsonNode> realm = realms.next();
				Map<String, T> targetMap = new HashMap<>(); 
				realmsMap.put(realm.getKey(), targetMap);
				ObjectNode targets = (ObjectNode) realm.getValue();
				Iterator<Entry<String, JsonNode>> targetsIt = targets.fields();
				while(targetsIt.hasNext())
				{
					Entry<String, JsonNode> target = targetsIt.next();
					targetMap.put(target.getKey(), deserializeValue(target.getValue()));
				}
			}
		} catch (Exception e)
		{
			TargetedPersistentIdentity.log.error("Can't deserialize state from JSON", e);
		}
	}

	protected abstract T deserializeValue(JsonNode node);
	protected abstract JsonNode serializeValue(T value);
	
	public void resetIdentities(String realm, String target)
	{
		if (realm == null)
		{
			for (String rrealm: realmsMap.keySet())
				resetRealm(rrealm, target);
		} else
			resetRealm(realm, target);
	}
	
	private void resetRealm(String realm, String target)
	{
		if (target == null)
			realmsMap.remove(realm);
		else
		{
			Map<String, T> realmMap = realmsMap.get(realm);
			if (realmMap != null)
				realmMap.remove(target);
		}
	}
	
	public void addIdentity(String realm, String target, T value)
	{
		Map<String, T> realmMap = realmsMap.get(realm);
		if (realmMap == null)
		{
			realmMap = new HashMap<>();
			realmsMap.put(realm, realmMap);
		}
		realmMap.put(target, value);
	}
	
	public T getIdentity(String realm, String target)
	{
		Map<String, T> realmMap = realmsMap.get(realm);
		if (realmMap == null)
			return null;
		return realmMap.get(target);
	}
	
	public String serialize()
	{
		try
		{
			ObjectNode root = mapper.createObjectNode();
			
			ObjectNode ids = root.putObject("mapOfIds");
			for (Entry<String, Map<String, T>> entry: realmsMap.entrySet())
			{
				ObjectNode targets = ids.putObject(entry.getKey());
				for (Entry<String, T> entry2: entry.getValue().entrySet())
					targets.put(entry2.getKey(), serializeValue(entry2.getValue()));
			}
			
			return mapper.writeValueAsString(root);
		} catch (Exception e)
		{
			TargetedPersistentIdentity.log.error("Can't serialize state to JSON", e);
			return null;
		}
	}
}