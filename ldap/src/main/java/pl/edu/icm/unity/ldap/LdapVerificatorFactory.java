/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.authn.CredentialVerificator;
import pl.edu.icm.unity.server.authn.CredentialVerificatorFactory;

/**
 * Produces verificators of passwords using remote LDAP server.
 * 
 * @author K. Benedyczak
 */
@Component
public class LdapVerificatorFactory implements CredentialVerificatorFactory
{
	public static final String NAME = "ldap";
	
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
		return new LdapVerificator(getName(), getDescription());
	}
}
