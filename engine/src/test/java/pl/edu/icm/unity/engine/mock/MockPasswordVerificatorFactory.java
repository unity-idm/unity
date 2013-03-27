/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificatorFactory;

@Component
public class MockPasswordVerificatorFactory implements LocalCredentialVerificatorFactory
{
	public static final String ID = "mockpassword";
	@Override
	public String getName()
	{
		return ID;
	}

	@Override
	public String getDescription()
	{
		return "Mock password verificator";
	}

	@Override
	public LocalCredentialVerificator newInstance()
	{
		return new MockPasswordVerificator(getName(), getDescription(), MockExchange.ID);
	}
}
