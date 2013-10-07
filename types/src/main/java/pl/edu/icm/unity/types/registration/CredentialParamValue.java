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
}
