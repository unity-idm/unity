/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.stdext.credential.PasswordVerificator;

/**
 * Produces raw password retrievals (and validation) for specific connectors.
 * @author K. Benedyczak
 */
@Component
public class RawPasswordRetrievalFactory implements CredentialRetrievalFactory
{
	public static final String NAME = "raw-password";
	
	@Autowired
	private IdentityResolver identityResolver;

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "RawPasswordRetrievalFactory.desc";
	}

	@Override
	public CredentialRetrieval newInstance()
	{
		return new RawPasswordRetrieval(identityResolver);
	}

	@Override
	public String getSupportedBinding()
	{
		return LdapServerAuthentication.NAME;
	}

	@Override
	public boolean isCredentialExchangeSupported(CredentialExchange e)
	{
		return e instanceof PasswordVerificator;
	}

}
