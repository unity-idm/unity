/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import pl.edu.icm.unity.server.api.internal.LoginSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Maintains targeted identities value. The data is stored per session identifiers. Additionally 
 * information about expiration handling is stored.
 * @author K. Benedyczak
 */
public class SessionIdentityModel
{
	private ObjectMapper mapper;
	private Map<String, PerSessionEntry> entries;
	
	public SessionIdentityModel(ObjectMapper mapper, JsonNode node)
	{
		this.mapper = mapper;
		entries = new HashMap<>();
		try
		{
			ObjectNode root = (ObjectNode) node;
			ObjectNode entriesN = (ObjectNode) root.get("entries");
			Iterator<Entry<String, JsonNode>> entriesIt = entriesN.fields();
			while(entriesIt.hasNext())
			{
				Entry<String, JsonNode> entryN = entriesIt.next();
				ObjectNode entryVal = (ObjectNode) entryN.getValue();
				PerSessionEntry entry = new PerSessionEntry(
						entryVal.get("absoluteTTL").asLong(), 
						entryVal.get("relativeTTL").asLong(),
						entryVal.get("lastUsage").asLong(),
						entryVal.get("identityValue").asText());
				entries.put(entryN.getKey(), entry);
			}
		} catch (Exception e)
		{
			TargetedPersistentIdentity.log.error("Can't deserialize state from JSON", e);
		}

	}

	public SessionIdentityModel(ObjectMapper mapper, LoginSession session, String identity)
	{
		this.mapper = mapper;
		entries = new HashMap<>();
		PerSessionEntry entry = new PerSessionEntry(
				session.getExpires() == null ? -1 : session.getExpires().getTime(), 
				session.getMaxInactivity()*10, 
				System.currentTimeMillis(), 
				identity);
		long msInHour = 3600000;
		if (entry.relativeTTL < 24*msInHour)
			entry.relativeTTL = 24*msInHour;
		
		entries.put(session.getId(), entry);
	}
	
	public ObjectNode serialize()
	{
		ObjectNode root = mapper.createObjectNode();
		ObjectNode entriesN = root.putObject("entries");
		for (Map.Entry<String, PerSessionEntry> e: entries.entrySet())
		{
			ObjectNode eN = entriesN.putObject(e.getKey());
			PerSessionEntry pse = e.getValue();
			eN.put("absoluteTTL", pse.absoluteTTL);
			eN.put("relativeTTL", pse.relativeTTL);
			eN.put("lastUsage", pse.lastUsage);
			eN.put("identityValue", pse.identityValue);
		}
		return root;
	}
	
	public String getIdentity(String sessionId)
	{
		PerSessionEntry entry = entries.get(sessionId);
		if (entry == null)
			return null;
		return entry.identityValue;
	}
	
	public void merge(SessionIdentityModel toMerge)
	{
		entries.putAll(toMerge.entries);
	}
	
	public void cleanup(long now)
	{
		Iterator<Map.Entry<String, PerSessionEntry>> it = entries.entrySet().iterator();
		while (it.hasNext())
		{
			PerSessionEntry ee = it.next().getValue();
			if (ee.absoluteTTL < now && ee.lastUsage + ee.relativeTTL < now)
				it.remove();
		}
	}
	
	public boolean isEmpty()
	{
		return entries.isEmpty();
	}
	
	private static class PerSessionEntry
	{
		private long absoluteTTL;
		private long relativeTTL;
		private long lastUsage;
		private String identityValue;

		public PerSessionEntry(long absoluteTTL, long relativeTTL, long lastUsage,
				String identityValue)
		{
			this.absoluteTTL = absoluteTTL;
			this.relativeTTL = relativeTTL;
			this.lastUsage = lastUsage;
			this.identityValue = identityValue;
		}
		
	}
}
