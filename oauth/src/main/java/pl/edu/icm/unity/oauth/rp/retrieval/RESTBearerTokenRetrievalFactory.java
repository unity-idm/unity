/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.retrieval;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.oauth.rp.AccessTokenExchange;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.CredentialRetrievalFactory;

/**
 * Factory of {@link RESTBearerTokenRetrieval}
 * @author K. Benedyczak
 */
@Component
public class RESTBearerTokenRetrievalFactory implements CredentialRetrievalFactory
{
	public static final String NAME = "rest-oauth-bearer";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "RESTBearerTokenRetrievalFactory.desc";
	}

	@Override
	public CredentialRetrieval newInstance()
	{
		return new RESTBearerTokenRetrieval();
	}

	@Override
	public String getSupportedBinding()
	{
		return JAXRSAuthentication.NAME;
	}

	@Override
	public boolean isCredentialExchangeSupported(CredentialExchange e)
	{
		return e instanceof AccessTokenExchange;
	}
}
