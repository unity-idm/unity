/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.local;

import java.util.Optional;

import pl.edu.icm.unity.engine.api.authn.AbstractVerificator;

/**
 * Abstract {@link LocalCredentialVerificator} with a common boilerplate code.
 * @author K. Benedyczak
 */
public abstract class AbstractLocalVerificator extends AbstractVerificator implements LocalCredentialVerificator
{
	protected String credentialName;
	private boolean supportingInvalidation;

	
	public AbstractLocalVerificator(String name, String description, String exchangeId, boolean supportingInvalidation)
	{
		super(name, description, exchangeId);
		this.supportingInvalidation = supportingInvalidation;
	}

	public String getCredentialName()
	{
		return credentialName;
	}

	public void setCredentialName(String credentialName)
	{
		this.credentialName = credentialName;
	}
	
	public boolean isSupportingInvalidation()
	{
		return supportingInvalidation;
	}

	public void setSupportingInvalidation(boolean supportingInvalidation)
	{
		this.supportingInvalidation = supportingInvalidation;
	}
	
	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Local;
	}
	
	@Override
	public Optional<String> updateCredentialAfterConfigurationChange(String currentCredential)
	{
		return Optional.empty();
	}
}
