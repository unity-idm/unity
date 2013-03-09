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
	private LocalAuthenticationState authenticationState;
	private Map<String, LocalCredentialState> credentialsState;
	
	
	public CredentialInfo(String credentialRequirementId, LocalAuthenticationState authenticationState,
			Map<String, LocalCredentialState> credentialsState)
	{
		this.credentialRequirementId = credentialRequirementId;
		this.credentialsState = credentialsState;
		this.authenticationState = authenticationState;
	}
	
	public String getCredentialRequirementId()
	{
		return credentialRequirementId;
	}
	public Map<String, LocalCredentialState> getCredentialsState()
	{
		return credentialsState;
	}

	public LocalAuthenticationState getAuthenticationState()
	{
		return authenticationState;
	}
}
