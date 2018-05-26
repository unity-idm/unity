/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;


/**
 * Produces raw password retrievals (and validation) for specific connectors.
 * @author K. Benedyczak
 */
@Component
public class LdapSimpleBindRetrievalFactory implements CredentialRetrievalFactory
{
	public static final String NAME = "ldap-simple";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "LdapSimpleBindRetrievalFactory.desc";
	}

	@Override
	public CredentialRetrieval newInstance()
	{
		return new LdapSimpleBindRetrieval();
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
