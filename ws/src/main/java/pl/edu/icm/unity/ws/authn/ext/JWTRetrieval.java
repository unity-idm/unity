/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.authn.ext;

import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.rest.jwt.authn.JWTRetrievalBase;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;

/**
 * Retrieves JWT token from the Authorized header.
 * @author K. Benedyczak
 */
public class JWTRetrieval extends JWTRetrievalBase implements JAXRSAuthentication
{
	public JWTRetrieval()
	{
		super(WebServiceAuthentication.NAME);
	}
}
