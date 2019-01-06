/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Describes a configured authenticator instance in implementation agnostic way.
 * <p>
 * Uses default JSON serialization.  
 * @author K. Benedyczak
 */
public class AuthenticatorInstanceMetadata
{
	private String id;
	private AuthenticatorTypeDescription typeDescription;
	private String configuration;
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
	public AuthenticatorInstanceMetadata clone()
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode json = mapper.convertValue(this, ObjectNode.class);
		return mapper.convertValue(json, AuthenticatorInstanceMetadata.class);
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
	
	public String getConfiguration()
	{
		return configuration;
	}
	
	public void setConfiguration(String configuration)
	{
		this.configuration = configuration;
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
		return Objects.hash(id, localCredentialName, revision, typeDescription, configuration);
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
		AuthenticatorInstanceMetadata other = (AuthenticatorInstanceMetadata) obj;
		return Objects.equals(id, other.id) && Objects.equals(localCredentialName, other.localCredentialName)
				&& revision == other.revision && Objects.equals(typeDescription, other.typeDescription)
				&& Objects.equals(configuration, other.configuration);
	}
}
