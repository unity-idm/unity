/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.authn.ext;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.rest.jwt.authn.JWTExchange;

/**
 * Factory of {@link JWTRetrieval}
 * @author K. Benedyczak
 */
@Component("SOAPJWTRetrievalFactory")
public class JWTRetrievalFactory implements CredentialRetrievalFactory
{
	public static final String NAME = "cxf-jwt";
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "CXFJWTRetrievalFactory.desc";
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
