/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

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
}
