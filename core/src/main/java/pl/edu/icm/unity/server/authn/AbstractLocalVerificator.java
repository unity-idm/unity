/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

/**
 * Abstract {@link LocalCredentialVerificator} with a common boilerplate code.
 * @author K. Benedyczak
 */
public abstract class AbstractLocalVerificator extends AbstractVerificator implements LocalCredentialVerificator
{
	protected String credentialName;
	
	public AbstractLocalVerificator(String name, String description, String exchangeId)
	{
		super(name, description, exchangeId);
	}

	public String getCredentialName()
	{
		return credentialName;
	}

	public void setCredentialName(String credentialName)
	{
		this.credentialName = credentialName;
	}
}
