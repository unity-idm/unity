/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Describes authenticator configuration. The configuration is binding agnostic and multiple instances 
 * of authenticators (binding specific) may be created from it.
 * <p>
 * Uses default JSON serialization.  
 * @author K. Benedyczak
 */
public class AuthenticatorInfo
{
	private String id;
	private AuthenticatorTypeDescription typeDescription;
	private String configuration;
	private Optional<String> localCredentialName;
	private Set<String> supportedBindings;

	public AuthenticatorInfo(String id, AuthenticatorTypeDescription typeDescription,
			String configuration, Optional<String> localCredentialName,
			Set<String> supportedBindings)
	{
		this.id = id;
		this.typeDescription = typeDescription;
		this.configuration = configuration;
		this.localCredentialName = localCredentialName;
		this.supportedBindings = supportedBindings;
	}

	//for JSON
	AuthenticatorInfo()
	{
	}

	public Optional<String> getLocalCredentialName()
	{
		return localCredentialName;
	}
	
	public String getId()
	{
		return id;
	}
	
	public AuthenticatorTypeDescription getTypeDescription()
	{
		return typeDescription;
	}
	
	public String getConfiguration()
	{
		return configuration;
	}

	public Set<String> getSupportedBindings()
	{
		return new HashSet<>(supportedBindings);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, localCredentialName, supportedBindings, typeDescription,
				configuration);
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
		AuthenticatorInfo other = (AuthenticatorInfo) obj;
		return Objects.equals(id, other.id) && Objects.equals(localCredentialName, other.localCredentialName)
				&& Objects.equals(supportedBindings, other.supportedBindings)
				&& Objects.equals(typeDescription, other.typeDescription)
				&& Objects.equals(configuration, other.configuration);
	}
}
