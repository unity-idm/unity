/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.server.authn.CredentialHelper;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificatorFactory;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;

/**
 * Produces verificators of passwords.
 * @author K. Benedyczak
 */
@Component
public class PasswordVerificatorFactory implements LocalCredentialVerificatorFactory
{
	public static final String NAME = "password";
	
	private NotificationProducer notificationProducer;
	private UnityServerConfiguration serverConfig;
	private CredentialHelper credentialHelper;
	
	
	@Autowired
	public PasswordVerificatorFactory(NotificationProducer notificationProducer,
			UnityServerConfiguration serverConfig, CredentialHelper credentialHelper)
	{
		this.notificationProducer = notificationProducer;
		this.serverConfig = serverConfig;
		this.credentialHelper = credentialHelper;
	}

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
		return new PasswordVerificator(getName(), getDescription(), notificationProducer,
				serverConfig, credentialHelper);
	}

	@Override
	public boolean isSupportingInvalidation()
	{
		return true;
	}
}
