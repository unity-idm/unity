/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;

public class MockPasswordRetrieval implements CredentialRetrieval, MockBinding
{
	private MockExchange exchange;
	
	@Override
	public String getBindingName()
	{
		return "web";
	}

	@Override
	public void setCredentialExchange(CredentialExchange e)
	{
		if (!(e instanceof MockExchange))
			throw new InternalException("Got unsupported exchange: " + 
					e.getClass() + " while only MockExchange is supported");
		this.exchange = (MockExchange) e;
	}

	@Override
	public String getSerializedConfiguration()
	{
		return "";
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
	}

	@Override
	public Long authenticate() throws EngineException
	{
		return exchange.checkPassword("CN=foo", "PPPbar");
	}
}
