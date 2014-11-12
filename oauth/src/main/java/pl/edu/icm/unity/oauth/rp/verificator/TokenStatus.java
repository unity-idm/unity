/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.verificator;

import java.util.Date;

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

	/**
	 * Invalid status
	 */
	public TokenStatus()
	{
		this.valid = false;
	}

	public TokenStatus(boolean valid, Date expirationTime, Scope scope, String subject)
	{
		this.valid = valid;
		this.expirationTime = expirationTime;
		this.scope = scope;
		this.subject = subject;
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
}
