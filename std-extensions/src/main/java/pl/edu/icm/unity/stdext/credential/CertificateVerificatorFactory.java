/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificatorFactory;

/**
 * Produces verificators of certificates.
 * @author K. Benedyczak
 */
@Component
public class CertificateVerificatorFactory implements LocalCredentialVerificatorFactory
{
	public static final String NAME = "certificate";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Verifies certificates";
	}

	@Override
	public LocalCredentialVerificator newInstance()
	{
		return new CertificateVerificator(getName(), getDescription());
	}

	@Override
	public boolean isSupportingInvalidation()
	{
		return false;
	}
}
