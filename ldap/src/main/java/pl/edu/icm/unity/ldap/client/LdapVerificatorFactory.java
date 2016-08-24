/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultProcessor;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;

/**
 * Produces verificators of passwords using remote LDAP server.
 * 
 * @author K. Benedyczak
 */
@Component
public class LdapVerificatorFactory implements CredentialVerificatorFactory
{
	public static final String NAME = "ldap";
	
	private PKIManagement pkiManagement;

	private RemoteAuthnResultProcessor processor;

	@Autowired
	public LdapVerificatorFactory(PKIManagement pkiManagement, RemoteAuthnResultProcessor processor)
	{
		this.pkiManagement = pkiManagement;
		this.processor = processor;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Verifies password using LDAPv3 protocol";
	}

	@Override
	public CredentialVerificator newInstance()
	{
		return new LdapVerificator(getName(), getDescription(), processor, pkiManagement,
				PasswordExchange.ID);
	}
}
