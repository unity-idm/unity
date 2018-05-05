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
import pl.edu.icm.unity.stdext.credential.cert.CertificateExchange;

/**
 * Produces pseudo verificators which search for and resolve attributes of an externally verified certificate 
 * (typically via authenticated TLS).
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class LdapCertVerificator extends LdapBaseVerificator
{
	public static final String NAME = "ldap-cert";
	public static final String DESCRIPTION = "Resolves certificate subject's information using LDAPv3 protocol";
	
	@Autowired
	public LdapCertVerificator(RemoteAuthnResultProcessor processor,
			PKIManagement pkiManagement)
	{
		super(NAME, DESCRIPTION, processor, pkiManagement, CertificateExchange.ID);
	}
	
	
	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<LdapCertVerificator> factory)
		{
			super(NAME, DESCRIPTION, factory);
		}
	}
}
