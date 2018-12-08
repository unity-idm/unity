/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.types;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import pl.edu.icm.unity.types.NamedObject;

/**
 * Configuration of authenticator allows for creating instances of authenticators from it.
 * 
 * @author K. Benedyczak
 */
public class AuthenticatorConfiguration implements NamedObject
{
	private String name;
	private String verificationMethod;
	private String configuration;
	private String localCredentialName;
	private long revision = 0;
	
	@JsonCreator
	public AuthenticatorConfiguration(
			@JsonProperty("name") String name, 
			@JsonProperty("verificationMethod") String verificationMethod, 
			@JsonProperty("configuration") String configuration,
			@JsonProperty("localCredentialName") String localCredentialName, 
			@JsonProperty("revision") long revision)
	{
		this.name = name;
		this.verificationMethod = verificationMethod;
		this.configuration = configuration;
		this.localCredentialName = localCredentialName;
		this.revision = revision;
	}

	@Override
	public String getName()
	{
		return name;
	}
	public String getVerificationMethod()
	{
		return verificationMethod;
	}
	public String getConfiguration()
	{
		return configuration;
	}
	public String getLocalCredentialName()
	{
		return localCredentialName;
	}
	public long getRevision()
	{
		return revision;
	}

	@Override
	public String toString()
	{
		return "AuthenticatorConfiguration [name=" + name + ", verificationMethod=" + verificationMethod
				+ ", configuration=" + configuration + ", localCredentialName=" + localCredentialName
				+ ", revision=" + revision + "]";
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(configuration, name, localCredentialName, revision, verificationMethod);
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
		AuthenticatorConfiguration other = (AuthenticatorConfiguration) obj;
		return Objects.equals(configuration, other.configuration) && Objects.equals(name, other.name)
				&& Objects.equals(localCredentialName, other.localCredentialName)
				&& revision == other.revision
				&& Objects.equals(verificationMethod, other.verificationMethod);
	}
}
