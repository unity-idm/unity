/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.util.Objects;

/**
 * Describes an available authenticator type. The contents is determined from available implementations, 
 * it is not modifiable at runtime.
 * <p>
 * Uses default JSON serialization.
 * 
 * @author K. Benedyczak
 */
public class AuthenticatorTypeDescription
{
	private String id;
	private String verificationMethod;
	private String verificationMethodDescription;
	private boolean local;

	public AuthenticatorTypeDescription(String id, String verificationMethod, String verificationMethodDescription,
			boolean local)
	{
		this.id = id;
		this.verificationMethod = verificationMethod;
		this.verificationMethodDescription = verificationMethodDescription;
		this.local = local;
	}

	public boolean isLocal()
	{
		return local;
	}

	public String getId()
	{
		return id;
	}

	public String getVerificationMethod()
	{
		return verificationMethod;
	}

	public String getVerificationMethodDescription()
	{
		return verificationMethodDescription;
	}

	@Override
	public String toString()
	{
		return "AuthenticatorTypeDescription [id=" + id + ", verificationMethod=" + verificationMethod
				+ ", verificationMethodDescription=" + verificationMethodDescription + ", local="
				+ local + "]";
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, local, verificationMethod, verificationMethodDescription);
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
		AuthenticatorTypeDescription other = (AuthenticatorTypeDescription) obj;
		return Objects.equals(id, other.id) && local == other.local
				&& Objects.equals(verificationMethod, other.verificationMethod)
				&& Objects.equals(verificationMethodDescription, other.verificationMethodDescription);
	}
}
