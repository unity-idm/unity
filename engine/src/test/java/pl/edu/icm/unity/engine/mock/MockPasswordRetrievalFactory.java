/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.CredentialRetrievalFactory;

@Component
public class MockPasswordRetrievalFactory implements CredentialRetrievalFactory
{
	@Override
	public String getName()
	{
		return "mockretrieval";
	}

	@Override
	public String getDescription()
	{
		return "Fake retrieval";
	}

	@Override
	public CredentialRetrieval newInstance()
	{
		return new MockPasswordRetrieval();
	}

	@Override
	public String getSupportedBinding()
	{
		return "web";
	}

	@Override
	public boolean isCredentialExchangeSupported(CredentialExchange e)
	{
		return MockExchange.class.isAssignableFrom(e.getClass());
	}

}
