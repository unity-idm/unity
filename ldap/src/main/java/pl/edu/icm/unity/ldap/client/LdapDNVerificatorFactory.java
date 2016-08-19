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
import pl.edu.icm.unity.stdext.credential.CertificateExchange;

/**
 * Produces pseudo verificators which search for and resolve attributes of an externally verified certificate 
 * (typically via authenticated TLS).
 * 
 * @author K. Benedyczak
 */
@Component
public class LdapDNVerificatorFactory implements CredentialVerificatorFactory
{
	public static final String NAME = "ldap-cert";
	
	private PKIManagement pkiManagement;

	private RemoteAuthnResultProcessor processor;

	@Autowired
	public LdapDNVerificatorFactory(RemoteAuthnResultProcessor processor, PKIManagement pkiManagement)
	{
		this.processor = processor;
		this.pkiManagement = pkiManagement;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Resolves certificate subject's information using LDAPv3 protocol";
	}

	@Override
	public CredentialVerificator newInstance()
	{
		return new LdapVerificator(getName(), getDescription(), processor, pkiManagement,
				CertificateExchange.ID);
	}
}
