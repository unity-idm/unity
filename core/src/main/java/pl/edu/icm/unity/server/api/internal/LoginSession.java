/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
}
