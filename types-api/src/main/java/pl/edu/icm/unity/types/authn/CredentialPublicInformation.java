/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;


/**
 * Stores information about credential: its status and credential-type dependent data, 
 * e.g. for password credential it may be its last update date.
 * @author K. Benedyczak
 */
public class CredentialPublicInformation
{
	private LocalCredentialState state;
	private String extraInformation;
	
	public CredentialPublicInformation(
			LocalCredentialState state, 
			String extraInformation)
	{
		this.state = state;
		this.extraInformation = extraInformation;
	}

	/**
	 * Used by JSON deserialization
	 */
	@SuppressWarnings("unused")
	private CredentialPublicInformation()
	{
	}

	public LocalCredentialState getState()
	{
		return state;
	}

	public String getExtraInformation()
	{
		return extraInformation;
	}
}
