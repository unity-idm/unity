/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.authn.CredentialVerificator;
import pl.edu.icm.unity.server.authn.CredentialVerificatorFactory;

@Component
public class MockPasswordVerificatorFactory implements CredentialVerificatorFactory
{
	@Override
	public String getName()
	{
		return "mockpasswd";
	}

	@Override
	public String getDescription()
	{
		return "Mock password verificator";
	}

	@Override
	public CredentialVerificator newInstance()
	{
		return new MockPasswordVerificator(getName(), getDescription(), MockExchange.ID);
	}
}
