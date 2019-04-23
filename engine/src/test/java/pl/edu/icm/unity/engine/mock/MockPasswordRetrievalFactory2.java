/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrievalFactory;

@Component
public class MockPasswordRetrievalFactory2 implements CredentialRetrievalFactory
{
	@Override
	public String getName()
	{
		return "mockretrieval2";
	}

	@Override
	public String getDescription()
	{
		return "Fake retrieval2";
	}

	@Override
	public CredentialRetrieval newInstance()
	{
		return new MockPasswordRetrieval();
	}

	@Override
	public String getSupportedBinding()
	{
		return "web2";
	}

	@Override
	public boolean isCredentialExchangeSupported(String e)
	{
		return MockExchange.ID.equals(e);
	}
}
