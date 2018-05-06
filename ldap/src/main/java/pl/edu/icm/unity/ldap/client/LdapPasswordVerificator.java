/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultProcessor;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExchange;

/**
 * Supports {@link PasswordExchange} and verifies the password and username against a configured LDAP 
 * server. Access to remote attributes and groups is also provided.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class LdapPasswordVerificator extends LdapBaseVerificator
{
	public static final String NAME = "ldap";
	public static final String DESCRIPTION = "Verifies password using LDAPv3 protocol";
	
	@Autowired
	public LdapPasswordVerificator(RemoteAuthnResultProcessor processor,
			PKIManagement pkiManagement)
	{
		super(NAME, DESCRIPTION, processor, pkiManagement, PasswordExchange.ID);
	}
	
	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<LdapPasswordVerificator> factory)
		{
			super(NAME, DESCRIPTION, factory);
		}
	}
}
