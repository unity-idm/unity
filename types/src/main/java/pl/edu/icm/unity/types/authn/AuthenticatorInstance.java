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
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((localCredentialName == null) ? 0
				: localCredentialName.hashCode());
		result = prime * result + ((retrievalJsonConfiguration == null) ? 0
				: retrievalJsonConfiguration.hashCode());
		result = prime * result
				+ ((typeDescription == null) ? 0 : typeDescription.hashCode());
		result = prime * result + ((verificatorJsonConfiguration == null) ? 0
				: verificatorJsonConfiguration.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AuthenticatorInstance other = (AuthenticatorInstance) obj;
		if (id == null)
		{
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (localCredentialName == null)
		{
			if (other.localCredentialName != null)
				return false;
		} else if (!localCredentialName.equals(other.localCredentialName))
			return false;
		if (retrievalJsonConfiguration == null)
		{
			if (other.retrievalJsonConfiguration != null)
				return false;
		} else if (!retrievalJsonConfiguration.equals(other.retrievalJsonConfiguration))
			return false;
		if (typeDescription == null)
		{
			if (other.typeDescription != null)
				return false;
		} else if (!typeDescription.equals(other.typeDescription))
			return false;
		if (verificatorJsonConfiguration == null)
		{
			if (other.verificatorJsonConfiguration != null)
				return false;
		} else if (!verificatorJsonConfiguration.equals(other.verificatorJsonConfiguration))
			return false;
		return true;
	}
}
