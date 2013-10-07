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
}
