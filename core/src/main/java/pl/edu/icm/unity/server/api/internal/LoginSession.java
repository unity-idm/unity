/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Represents login session. Session expiration can be stored in two ways: either
 * to expire after a certain time of inactivity is reached or when an absolute point in time is reached.
 * The first case is the typical one. The latter is used when user's session should be preserved between  
 * browser shutdowns.
 * <p>
 * In the absolute termination time the maxInactivity time is also used, but only after the 
 * absolute expiry time has passed. This prevents killing such session when it is being used.
 * 
 * @author K. Benedyczak
 */
public class LoginSession
{
	private String id;
	private Date started;
	private Date expires;
	private Date lastUsed;
	private long maxInactivity;
	private long entityId;
	private String realm;
	private boolean usedOutdatedCredential;
	private String entityLabel;
	
	private Map<String, String> sessionData = new HashMap<String, String>();

	public LoginSession()
	{
	}

	/**
	 * Construct a session with absolute expiration.
	 * @param id
	 * @param started
	 * @param expires
	 * @param maxInactivity
	 * @param entityId
	 * @param realm
	 */
	public LoginSession(String id, Date started, Date expires, long maxInactivity, long entityId, String realm)
	{
		this.id = id;
		this.started = started;
		this.entityId = entityId;
		this.realm = realm;
		this.lastUsed = new Date();
		this.expires = expires;
		this.maxInactivity = maxInactivity;
	}

	/**
	 * Constructs a session with relative expiration
	 * @param id
	 * @param started
	 * @param maxInactivity
	 * @param entityId
	 * @param realm
	 */
	public LoginSession(String id, Date started, long maxInactivity, long entityId, String realm)
	{
		this(id, started, null, maxInactivity, entityId, realm);
	}

	
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public Date getStarted()
	{
		return started;
	}
	public void setStarted(Date started)
	{
		this.started = started;
	}
	public Date getExpires()
	{
		return expires;
	}
	public void setExpires(Date expires)
	{
		this.expires = expires;
	}
	public long getEntityId()
	{
		return entityId;
	}
	public void setEntityId(long entityId)
	{
		this.entityId = entityId;
	}
	public String getRealm()
	{
		return realm;
	}
	public void setRealm(String realm)
	{
		this.realm = realm;
	}
	public Map<String, String> getSessionData()
	{
		return sessionData;
	}
	public void setSessionData(Map<String, String> sessionData)
	{
		this.sessionData = sessionData;
	}

	public Date getLastUsed()
	{
		return lastUsed;
	}

	public void setLastUsed(Date lastUsed)
	{
		this.lastUsed = lastUsed;
	}

	public long getMaxInactivity()
	{
		return maxInactivity;
	}

	public void setMaxInactivity(long maxInactivity)
	{
		this.maxInactivity = maxInactivity;
	}

	public boolean isUsedOutdatedCredential()
	{
		return usedOutdatedCredential;
	}

	public void setUsedOutdatedCredential(boolean usedOutdatedCredential)
	{
		this.usedOutdatedCredential = usedOutdatedCredential;
	}

	public String getEntityLabel()
	{
		return entityLabel;
	}

	public void setEntityLabel(String entityLabel)
	{
		this.entityLabel = entityLabel;
	}
	
	public void deserialize(Token token)
	{
		ObjectNode main;
		try
		{
			main = Constants.MAPPER.readValue(token.getContents(), ObjectNode.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}

		String realm = main.get("realm").asText();
		long maxInactive = main.get("maxInactivity").asLong();
		long lastUsed = main.get("lastUsed").asLong();
		String entityLabel = main.get("entityLabel").asText();
		boolean outdatedCred = main.get("usedOutdatedCredential").asBoolean();
		
		setId(token.getValue());
		setStarted(token.getCreated());
		setExpires(token.getExpires());
		setMaxInactivity(maxInactive);
		setEntityId(token.getOwner());
		setRealm(realm);
		setLastUsed(new Date(lastUsed));
		setEntityLabel(entityLabel);
		setUsedOutdatedCredential(outdatedCred);
		
		Map<String, String> attrs = new HashMap<String, String>(); 
		ObjectNode attrsJson = (ObjectNode) main.get("attributes");
		Iterator<String> fNames = attrsJson.fieldNames();
		while (fNames.hasNext())
		{
			String attrName = fNames.next();
			attrs.put(attrName, attrsJson.get(attrName).asText());
		}
		setSessionData(attrs);
	}
	
	public byte[] getTokenContents()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("realm", getRealm());
		main.put("maxInactivity", getMaxInactivity());
		main.put("lastUsed", getLastUsed().getTime());
		main.put("usedOutdatedCredential", isUsedOutdatedCredential());
		main.put("entityLabel", getEntityLabel());
		
		ObjectNode attrsJson = main.putObject("attributes");
		for (Map.Entry<String, String> a: getSessionData().entrySet())
			attrsJson.put(a.getKey(), a.getValue());

		try
		{
			return Constants.MAPPER.writeValueAsBytes(main);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}
}
