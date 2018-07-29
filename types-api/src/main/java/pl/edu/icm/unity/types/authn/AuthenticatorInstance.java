/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.types.NamedObject;

/**
 * Describes a configured authenticator instance in implementation agnostic way.
 * <p>
 * Uses default JSON serialization.  
 * @author K. Benedyczak
 */
public class AuthenticatorInstance implements NamedObject
{
	private String id;
	private AuthenticatorTypeDescription typeDescription;
	private String retrievalConfiguration;
	private String verificatorConfiguration;
	private String localCredentialName;
	private long revision = 0;
	
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
	
	@Override
	public AuthenticatorInstance clone()
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode json = mapper.convertValue(this, ObjectNode.class);
		return mapper.convertValue(json, AuthenticatorInstance.class);
	}
	
	@JsonIgnore
	@Override
	public String getName()
	{
		return getId();
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
	
	public String getRetrievalConfiguration()
	{
		return retrievalConfiguration;
	}
	
	public void setRetrievalConfiguration(String retrievalJsonConfiguration)
	{
		this.retrievalConfiguration = retrievalJsonConfiguration;
	}
	
	public String getVerificatorConfiguration()
	{
		return verificatorConfiguration;
	}
	
	public void setVerificatorConfiguration(String verificatorJsonConfiguration)
	{
		this.verificatorConfiguration = verificatorJsonConfiguration;
	}
	
	public long getRevision()
	{
		return revision;
	}
	
	public void setRevision(long revision)
	{
		this.revision = revision;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((localCredentialName == null) ? 0
				: localCredentialName.hashCode());
		result = prime * result + ((retrievalConfiguration == null) ? 0
				: retrievalConfiguration.hashCode());
		result = prime * result
				+ ((typeDescription == null) ? 0 : typeDescription.hashCode());
		result = prime * result + ((verificatorConfiguration == null) ? 0
				: verificatorConfiguration.hashCode());
		result = prime * result + (int) (revision ^ (revision >>> 32));
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
		if (retrievalConfiguration == null)
		{
			if (other.retrievalConfiguration != null)
				return false;
		} else if (!retrievalConfiguration.equals(other.retrievalConfiguration))
			return false;
		if (typeDescription == null)
		{
			if (other.typeDescription != null)
				return false;
		} else if (!typeDescription.equals(other.typeDescription))
			return false;
		if (verificatorConfiguration == null)
		{
			if (other.verificatorConfiguration != null)
				return false;
		} else if (!verificatorConfiguration.equals(other.verificatorConfiguration))
			return false;
		if (revision != other.revision)
			return false;
		
		return true;
	}
}
