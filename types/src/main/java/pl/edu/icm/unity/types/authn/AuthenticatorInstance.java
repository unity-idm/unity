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
	private String retrievalJsonConfiguration;
	private String verificatorJsonConfiguration;
	private String localCredentialName;
	
	/**
	 * @return name of the local credential bound to this authenticator. In case of non local authenticators null is returned
	 */
	public String getLocalCredentialName()
	{
		return localCredentialName;
	}
	public void setLocalCredentialName(String localCredentialName)
	{
		this.localCredentialName = localCredentialName;
	}
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
	public String getRetrievalJsonConfiguration()
	{
		return retrievalJsonConfiguration;
	}
	public void setRetrievalJsonConfiguration(String retrievalJsonConfiguration)
	{
		this.retrievalJsonConfiguration = retrievalJsonConfiguration;
	}
	public String getVerificatorJsonConfiguration()
	{
		return verificatorJsonConfiguration;
	}
	public void setVerificatorJsonConfiguration(String verificatorJsonConfiguration)
	{
		this.verificatorJsonConfiguration = verificatorJsonConfiguration;
	}
}
