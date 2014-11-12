/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.retrieval;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.oauth.rp.AccessTokenExchange;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;

/**
 * Factory of {@link SOAPBearerTokenRetrieval}
 * @author K. Benedyczak
 */
@Component
public class SOAPBearerTokenRetrievalFactory implements CredentialRetrievalFactory
{
	public static final String NAME = "cxf-oauth-bearer";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "CXFBearerTokenRetrievalFactory.desc";
	}

	@Override
	public CredentialRetrieval newInstance()
	{
		return new SOAPBearerTokenRetrieval();
	}

	@Override
	public String getSupportedBinding()
	{
		return WebServiceAuthentication.NAME;
	}

	@Override
	public boolean isCredentialExchangeSupported(CredentialExchange e)
	{
		return e instanceof AccessTokenExchange;
	}
}
