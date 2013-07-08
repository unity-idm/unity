/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

/**
 * Entity and a value of (one of) its credential attribute 
 * @author K. Benedyczak
 */
public class EntityWithCredential
{
	private String credentialName;
	private String credentialValue;
	private long entityId;
	
	public String getCredentialName()
	{
		return credentialName;
	}
	public void setCredentialName(String credentialName)
	{
		this.credentialName = credentialName;
	}
	public String getCredentialValue()
	{
		return credentialValue;
	}
	public void setCredentialValue(String credentialValue)
	{
		this.credentialValue = credentialValue;
	}
	public long getEntityId()
	{
		return entityId;
	}
	public void setEntityId(long entityId)
	{
		this.entityId = entityId;
	}
}
