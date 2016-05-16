/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Credential registration option.
 * @author K. Benedyczak
 */
public class CredentialRegistrationParam
{
	private String credentialName;
	private String label;
	private String description;
	
	public CredentialRegistrationParam(String credentialName, String label, String description)
	{
		this.credentialName = credentialName;
		this.label = label;
		this.description = description;
	}

	public CredentialRegistrationParam(String credentialName)
	{
		this.credentialName = credentialName;
	}

	public CredentialRegistrationParam()
	{
	}

	
	public String getCredentialName()
	{
		return credentialName;
	}
	public void setCredentialName(String credentialName)
	{
		this.credentialName = credentialName;
	}
	public String getLabel()
	{
		return label;
	}
	public void setLabel(String label)
	{
		this.label = label;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((credentialName == null) ? 0 : credentialName.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
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
		CredentialRegistrationParam other = (CredentialRegistrationParam) obj;
		if (credentialName == null)
		{
			if (other.credentialName != null)
				return false;
		} else if (!credentialName.equals(other.credentialName))
			return false;
		if (description == null)
		{
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (label == null)
		{
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		return true;
	}
}
