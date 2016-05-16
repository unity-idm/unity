/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Credential registration parameter
 * @author K. Benedyczak
 */
public class CredentialParamValue
{
	private String credentialId;
	private String secrets;
	public String getCredentialId()
	{
		return credentialId;
	}
	public void setCredentialId(String credentialId)
	{
		this.credentialId = credentialId;
	}
	public String getSecrets()
	{
		return secrets;
	}
	public void setSecrets(String secrets)
	{
		this.secrets = secrets;
	}
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((credentialId == null) ? 0 : credentialId.hashCode());
		result = prime * result + ((secrets == null) ? 0 : secrets.hashCode());
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
		CredentialParamValue other = (CredentialParamValue) obj;
		if (credentialId == null)
		{
			if (other.credentialId != null)
				return false;
		} else if (!credentialId.equals(other.credentialId))
			return false;
		if (secrets == null)
		{
			if (other.secrets != null)
				return false;
		} else if (!secrets.equals(other.secrets))
			return false;
		return true;
	}
}
