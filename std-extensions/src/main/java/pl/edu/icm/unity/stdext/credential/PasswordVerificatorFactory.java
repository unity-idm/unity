/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificatorFactory;

/**
 * Produces verificators of passwords.
 * @author K. Benedyczak
 */
@Component
public class PasswordVerificatorFactory implements LocalCredentialVerificatorFactory
{
	public static final String NAME = "password";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Verifies passwords";
	}

	@Override
	public LocalCredentialVerificator newInstance()
	{
		return new PasswordVerificator(getName(), getDescription());
	}

}
