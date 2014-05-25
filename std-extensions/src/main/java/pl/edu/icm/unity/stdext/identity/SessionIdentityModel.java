/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import pl.edu.icm.unity.server.api.internal.LoginSession;

import com.fasterxml.jackson.core.JsonProcessingException;
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
	private PerSessionEntry entry;
	
	public SessionIdentityModel(ObjectMapper mapper, String node)
	{
		this.mapper = mapper;
		try
		{
			ObjectNode entryVal = (ObjectNode) mapper.readTree(node);
			entry = new PerSessionEntry(
					entryVal.get("absoluteTTL").asLong(), 
					entryVal.get("relativeTTL").asLong(),
					entryVal.get("lastUsage").asLong());
		} catch (Exception e)
		{
			TargetedPersistentIdentity.log.error("Can't deserialize state from JSON", e);
		}

	}

	public SessionIdentityModel(ObjectMapper mapper, LoginSession session, String identity)
	{
		this.mapper = mapper;
		entry = new PerSessionEntry(
				session.getExpires() == null ? -1 : session.getExpires().getTime(), 
				session.getMaxInactivity()*10, 
				System.currentTimeMillis());
		long msInHour = 3600000;
		if (entry.relativeTTL < 24*msInHour)
			entry.relativeTTL = 24*msInHour;
	}
	
	public String serialize()
	{
		ObjectNode eN = mapper.createObjectNode();
		eN.put("absoluteTTL", entry.absoluteTTL);
		eN.put("relativeTTL", entry.relativeTTL);
		eN.put("lastUsage", entry.lastUsage);
		try
		{
			return mapper.writeValueAsString(eN);
		} catch (JsonProcessingException e1)
		{
			throw new IllegalStateException("Can't serialize transient identity value to JSON", e1);
		}
	}
	
	public PerSessionEntry getEntry()
	{
		return entry;
	}


	public static class PerSessionEntry
	{
		private long absoluteTTL;
		private long relativeTTL;
		private long lastUsage;

		public PerSessionEntry(long absoluteTTL, long relativeTTL, long lastUsage)
		{
			this.absoluteTTL = absoluteTTL;
			this.relativeTTL = relativeTTL;
			this.lastUsage = lastUsage;
		}
		
		public boolean isExpired()
		{
			long now = System.currentTimeMillis();
			return absoluteTTL < now || relativeTTL < now-lastUsage;
		}
	}
}
