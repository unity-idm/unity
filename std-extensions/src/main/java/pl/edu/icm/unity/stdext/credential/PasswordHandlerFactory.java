/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.authn.AbstractLocalCredentialHandlerFactory;
import pl.edu.icm.unity.server.authn.LocalCredentialHandler;

/**
 * Factory for the ordinary password credentials.
 * @author K. Benedyczak
 */
@Component
public class PasswordHandlerFactory  extends AbstractLocalCredentialHandlerFactory
{
	public static final String ID = "password";
	
	protected PasswordHandlerFactory()
	{
		super(ID, "Normal password credential");
	}

	@Override
	public LocalCredentialHandler newInstance()
	{
		return new PasswordHandler(type);
	}

}
