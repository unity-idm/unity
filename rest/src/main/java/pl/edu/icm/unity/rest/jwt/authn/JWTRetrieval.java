/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.jwt.authn;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;

/**
 * Retrieves JWT token from the Authorized header.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class JWTRetrieval extends JWTRetrievalBase implements JAXRSAuthentication
{
	public static final String NAME = "rest-jwt";
	public static final String DESC = "RESTJWTRetrievalFactory.desc";
	
	public JWTRetrieval()
	{
		super(JAXRSAuthentication.NAME);
	}
	
	@Component
	public static class Factory extends AbstractCredentialRetrievalFactory<JWTRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<JWTRetrieval> factory)
		{
			super(NAME, DESC, JAXRSAuthentication.NAME, factory, JWTExchange.ID);
		}
	}

}
