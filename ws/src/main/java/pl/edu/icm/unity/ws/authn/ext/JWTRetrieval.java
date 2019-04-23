/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.authn.ext;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.rest.jwt.authn.JWTExchange;
import pl.edu.icm.unity.rest.jwt.authn.JWTRetrievalBase;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;

/**
 * Retrieves JWT token from the Authorized header.
 * @author K. Benedyczak
 */
@Component("SOAPJWTRetrieval")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class JWTRetrieval extends JWTRetrievalBase implements JAXRSAuthentication
{
	public static final String NAME = "cxf-jwt";
	public static final String DESC = "CXFJWTRetrievalFactory.desc";

	public JWTRetrieval()
	{
		super(WebServiceAuthentication.NAME);
	}
	
	@Component("SOAPJWTRetrievalFactory")
	public static class Factory extends AbstractCredentialRetrievalFactory<JWTRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<JWTRetrieval> factory)
		{
			super(NAME, DESC, JAXRSAuthentication.NAME, factory, JWTExchange.ID);
		}
	}
}
