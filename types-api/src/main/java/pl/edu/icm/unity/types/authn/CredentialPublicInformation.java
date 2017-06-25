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

	@Override
	public String toString()
	{
		return "CredentialPublicInformation [state=" + state + ", extraInformation="
				+ extraInformation + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((extraInformation == null) ? 0 : extraInformation.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
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
		CredentialPublicInformation other = (CredentialPublicInformation) obj;
		if (extraInformation == null)
		{
			if (other.extraInformation != null)
				return false;
		} else if (!extraInformation.equals(other.extraInformation))
			return false;
		if (state != other.state)
			return false;
		return true;
	}
}
