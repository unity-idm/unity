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
	private Map<String, LocalCredentialState> credentialsState;
	
	public CredentialInfo(String credentialRequirementId,
			Map<String, LocalCredentialState> credentialsState)
	{
		this.credentialRequirementId = credentialRequirementId;
		this.credentialsState = credentialsState;
	}
	
	public String getCredentialRequirementId()
	{
		return credentialRequirementId;
	}
	public Map<String, LocalCredentialState> getCredentialsState()
	{
		return credentialsState;
	}
}
