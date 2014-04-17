/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn.ext;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;

/**
 * Factory of {@link HttpBasicRetrieval}
 * @author K. Benedyczak
 */
@Component
public class HttpBasicRetrievalFactory implements CredentialRetrievalFactory
{
	public static final String NAME = "rest-httpbasic";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "CXFHttpBasicRetrievalFactory.desc";
	}

	@Override
	public CredentialRetrieval newInstance()
	{
		return new HttpBasicRetrieval();
	}

	@Override
	public String getSupportedBinding()
	{
		return JAXRSAuthentication.NAME;
	}

	@Override
	public boolean isCredentialExchangeSupported(CredentialExchange e)
	{
		return e instanceof PasswordExchange;
	}
}
