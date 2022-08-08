/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.verificator;

import java.util.Date;
import java.util.Optional;

import com.nimbusds.oauth2.sdk.Scope;

/**
 * Token status information.
 * @author K. Benedyczak
 */
public class TokenStatus
{
	private boolean valid;
	private Date expirationTime;
	private Scope scope;
	private String subject;
	private Optional<Long> clientId;
	private Optional<Long> ownerId;


	/**
	 * Invalid status
	 */
	public TokenStatus()
	{
		this.valid = false;
	}

	public TokenStatus(boolean valid, Date expirationTime, Scope scope, String subject)
	{
		this(valid, expirationTime, scope, subject, null, null);
	}
	
	public TokenStatus(boolean valid, Date expirationTime, Scope scope, String subject, Long ownerId, Long clientId)
	{
		this.valid = valid;
		this.expirationTime = expirationTime;
		this.scope = scope;
		this.subject = subject;
		this.setClientId(Optional.ofNullable(clientId));
		this.setOwnerId(Optional.ofNullable(ownerId));
	}

	public boolean isValid()
	{
		return valid;
	}
	public void setValid(boolean valid)
	{
		this.valid = valid;
	}
	public Date getExpirationTime()
	{
		return expirationTime;
	}
	public void setExpirationTime(Date expirationTime)
	{
		this.expirationTime = expirationTime;
	}
	public Scope getScope()
	{
		return scope;
	}
	public void setScope(Scope scope)
	{
		this.scope = scope;
	}

	public String getSubject()
	{
		return subject;
	}

	public void setSubject(String subject)
	{
		this.subject = subject;
	}

	public Optional<Long> getClientId()
	{
		return clientId;
	}

	public void setClientId(Optional<Long> clientId)
	{
		this.clientId = clientId;
	}

	public Optional<Long> getOwnerId()
	{
		return ownerId;
	}

	public void setOwnerId(Optional<Long> ownerId)
	{
		this.ownerId = ownerId;
	}

	
}
