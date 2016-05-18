/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

/**
 * Describes an available authenticator. The contents is determined from implementations,
 * it is not modifiable at runtime.
 * <p>
 * Uses default JSON serialization.
 * @author K. Benedyczak
 */
public class AuthenticatorTypeDescription
{
	private String id;
	private String supportedBinding;
	private String verificationMethod;
	private String verificationMethodDescription;
	private String retrievalMethod;
	private String retrievalMethodDescription;
	private boolean local;
	
	
	
	public boolean isLocal()
	{
		return local;
	}
	public void setLocal(boolean local)
	{
		this.local = local;
	}
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public String getSupportedBinding()
	{
		return supportedBinding;
	}
	public void setSupportedBinding(String supportedBinding)
	{
		this.supportedBinding = supportedBinding;
	}
	public String getVerificationMethod()
	{
		return verificationMethod;
	}
	public void setVerificationMethod(String verificationMethod)
	{
		this.verificationMethod = verificationMethod;
	}
	public String getVerificationMethodDescription()
	{
		return verificationMethodDescription;
	}
	public void setVerificationMethodDescription(String verificationMethodDescription)
	{
		this.verificationMethodDescription = verificationMethodDescription;
	}
	public String getRetrievalMethod()
	{
		return retrievalMethod;
	}
	public void setRetrievalMethod(String retrievalMethod)
	{
		this.retrievalMethod = retrievalMethod;
	}
	public String getRetrievalMethodDescription()
	{
		return retrievalMethodDescription;
	}
	public void setRetrievalMethodDescription(String retrievalMethodDescription)
	{
		this.retrievalMethodDescription = retrievalMethodDescription;
	}
	@Override
	public String toString()
	{
		return "id=" + id + ", supportedBinding="
				+ supportedBinding + ", verificationMethod=" + verificationMethod
				+ ", verificationMethodDescription="
				+ verificationMethodDescription + ", retrievalMethod="
				+ retrievalMethod + ", retrievalMethodDescription="
				+ retrievalMethodDescription + ", local=" + local;
	}
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + (local ? 1231 : 1237);
		result = prime * result
				+ ((retrievalMethod == null) ? 0 : retrievalMethod.hashCode());
		result = prime * result + ((retrievalMethodDescription == null) ? 0
				: retrievalMethodDescription.hashCode());
		result = prime * result
				+ ((supportedBinding == null) ? 0 : supportedBinding.hashCode());
		result = prime * result + ((verificationMethod == null) ? 0
				: verificationMethod.hashCode());
		result = prime * result + ((verificationMethodDescription == null) ? 0
				: verificationMethodDescription.hashCode());
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
		AuthenticatorTypeDescription other = (AuthenticatorTypeDescription) obj;
		if (id == null)
		{
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (local != other.local)
			return false;
		if (retrievalMethod == null)
		{
			if (other.retrievalMethod != null)
				return false;
		} else if (!retrievalMethod.equals(other.retrievalMethod))
			return false;
		if (retrievalMethodDescription == null)
		{
			if (other.retrievalMethodDescription != null)
				return false;
		} else if (!retrievalMethodDescription.equals(other.retrievalMethodDescription))
			return false;
		if (supportedBinding == null)
		{
			if (other.supportedBinding != null)
				return false;
		} else if (!supportedBinding.equals(other.supportedBinding))
			return false;
		if (verificationMethod == null)
		{
			if (other.verificationMethod != null)
				return false;
		} else if (!verificationMethod.equals(other.verificationMethod))
			return false;
		if (verificationMethodDescription == null)
		{
			if (other.verificationMethodDescription != null)
				return false;
		} else if (!verificationMethodDescription
				.equals(other.verificationMethodDescription))
			return false;
		return true;
	}
}
