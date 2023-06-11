/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.authn;

import java.util.Map;

/**
 * Stores information about entity authentication: the id of {@link CredentialRequirements} and the state.
 * @author K. Benedyczak
 */
public class CredentialInfo
{
	private String credentialRequirementId;
	private Map<String, CredentialPublicInformation> credentialsState;
	
	public CredentialInfo(String credentialRequirementId, Map<String, CredentialPublicInformation> credentialsState)
	{
		this.credentialRequirementId = credentialRequirementId;
		this.credentialsState = credentialsState;
	}
	
	/**
	 * Used by Jackson during deserialization
	 */
	@SuppressWarnings("unused")
	private CredentialInfo()
	{
	}
	
	public String getCredentialRequirementId()
	{
		return credentialRequirementId;
	}
	
	public Map<String, CredentialPublicInformation> getCredentialsState()
	{
		return credentialsState;
	}

	@Override
	public String toString()
	{
		return "CredentialInfo [credentialRequirementId=" + credentialRequirementId
				+ ", credentialsState=" + credentialsState + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((credentialRequirementId == null) ? 0
				: credentialRequirementId.hashCode());
		result = prime * result
				+ ((credentialsState == null) ? 0 : credentialsState.hashCode());
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
		CredentialInfo other = (CredentialInfo) obj;
		if (credentialRequirementId == null)
		{
			if (other.credentialRequirementId != null)
				return false;
		} else if (!credentialRequirementId.equals(other.credentialRequirementId))
			return false;
		if (credentialsState == null)
		{
			if (other.credentialsState != null)
				return false;
		} else if (!credentialsState.equals(other.credentialsState))
			return false;
		return true;
	}
}
