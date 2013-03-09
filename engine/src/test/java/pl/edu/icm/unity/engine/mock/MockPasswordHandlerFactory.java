/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.authn.AbstractLocalCredentialHandlerFactory;
import pl.edu.icm.unity.server.authn.LocalCredentialHandler;

@Component
public class MockPasswordHandlerFactory extends AbstractLocalCredentialHandlerFactory
{
	public static final String ID = "simple password";
	public MockPasswordHandlerFactory()
	{
		super(ID, "Simple, insecure password credential");
	}

	@Override
	public LocalCredentialHandler newInstance()
	{
		return new MockPasswordHandler(type);
	}
}
