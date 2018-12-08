/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.util.Objects;

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
		return Objects.hash(id, localCredentialName, revision, typeDescription, verificatorConfiguration);
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
		return Objects.equals(id, other.id) && Objects.equals(localCredentialName, other.localCredentialName)
				&& revision == other.revision && Objects.equals(typeDescription, other.typeDescription)
				&& Objects.equals(verificatorConfiguration, other.verificatorConfiguration);
	}
}
