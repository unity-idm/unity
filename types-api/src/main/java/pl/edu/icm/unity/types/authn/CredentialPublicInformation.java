/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.util.Objects;

/**
 * Stores information about credential: its status and credential-type dependent data, 
 * e.g. for password credential it may be its last update date.
 * @author K. Benedyczak
 */
public class CredentialPublicInformation
{
	private LocalCredentialState state;
	private String stateDetail;
	private String extraInformation;
	
	public CredentialPublicInformation(
			LocalCredentialState state, 
			String extraInformation)
	{
		this.state = state;
		this.extraInformation = extraInformation;
	}

	public CredentialPublicInformation(
			LocalCredentialState state,
			String stateDetail,
			String extraInformation)
	{
		this.state = state;
		this.stateDetail = stateDetail;
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

	public String getStateDetail()
	{
		return stateDetail;
	}

	@Override
	public String toString()
	{
		return "CredentialPublicInformation [state=" + state + ", stateDetail=" + stateDetail
				+ ", extraInformation=" + extraInformation + "]";
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(extraInformation, state, stateDetail);
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
		CredentialPublicInformation other = (CredentialPublicInformation) obj;
		return Objects.equals(extraInformation, other.extraInformation) && state == other.state
				&& Objects.equals(stateDetail, other.stateDetail);
	}
}
