/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents login session.
 * 
 * @author K. Benedyczak
 */
public class LoginSession
{
	private String id;
	private Date started;
	private Date expires;
	private long entityId;
	private String realm;
	private Map<String, String> sessionData = new HashMap<String, String>();

	public LoginSession()
	{
	}


	public LoginSession(String id, Date started, Date expires, long entityId, String realm)
	{
		this.id = id;
		this.started = started;
		this.expires = expires;
		this.entityId = entityId;
		this.realm = realm;
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
}
