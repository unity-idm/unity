/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;


/**
 * Describes a configured authenticator instance in implementation agnostic way.  
 * @author K. Benedyczak
 */
public class AuthenticatorInstance
{
	private String id;
	private AuthenticatorTypeDescription typeDescription;
	private String verificatorId;
	private String jsonConfiguration;
	private String retrievalId;
	
	
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public AuthenticatorTypeDescription getTypeDescription()
	{
		return typeDescription;
	}
	public void setTypeDescription(AuthenticatorTypeDescription typeDescription)
	{
		this.typeDescription = typeDescription;
	}
	public String getVerificatorId()
	{
		return verificatorId;
	}
	public void setVerificatorId(String verificatorId)
	{
		this.verificatorId = verificatorId;
	}
	public String getJsonConfiguration()
	{
		return jsonConfiguration;
	}
	public void setJsonConfiguration(String jsonConfiguration)
	{
		this.jsonConfiguration = jsonConfiguration;
	}
	public String getRetrievalId()
	{
		return retrievalId;
	}
	public void setRetrievalId(String retrievalId)
	{
		this.retrievalId = retrievalId;
	}
}
