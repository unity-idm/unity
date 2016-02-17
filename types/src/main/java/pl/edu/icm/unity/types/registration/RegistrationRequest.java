/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;


/**
 * Registration request, tied to a registration form contains data collected
 * during registration process. This data can be entered by the user in UI,
 * taken from external IdP or possibly from other sources (e.g. a DN can be
 * taken from client-authenticated TLS).
 * 
 * @author K. Benedyczak
 */
public class RegistrationRequest extends BaseRegistrationInput
{
	private String registrationCode;
	
	public String getRegistrationCode()
	{
		return registrationCode;
	}

	public void setRegistrationCode(String registrationCode)
	{
		this.registrationCode = registrationCode;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((registrationCode == null) ? 0 : registrationCode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		RegistrationRequest other = (RegistrationRequest) obj;
		if (registrationCode == null)
		{
			if (other.registrationCode != null)
				return false;
		} else if (!registrationCode.equals(other.registrationCode))
			return false;
		return true;
	}
}
