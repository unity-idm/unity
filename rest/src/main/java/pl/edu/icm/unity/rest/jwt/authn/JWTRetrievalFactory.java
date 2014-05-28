/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.jwt.authn;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.CredentialRetrievalFactory;

/**
 * Factory of {@link JWTRetrieval}
 * @author K. Benedyczak
 */
@Component
public class JWTRetrievalFactory implements CredentialRetrievalFactory
{
	public static final String NAME = "rest-jwt";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "RESTJWTRetrievalFactory.desc";
	}

	@Override
	public CredentialRetrieval newInstance()
	{
		return new JWTRetrieval();
	}

	@Override
	public String getSupportedBinding()
	{
		return JAXRSAuthentication.NAME;
	}

	@Override
	public boolean isCredentialExchangeSupported(CredentialExchange e)
	{
		return e instanceof JWTExchange;
	}
}
