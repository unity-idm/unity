/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn;

import pl.edu.icm.unity.rest.RESTEndpoint;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;

/**
 * Defines a contract which must be implemented by {@link CredentialRetrieval}s in order to be used 
 * with the {@link RESTEndpoint}.
 * 
 * @author K. Benedyczak
 */
public interface JAXRSAuthentication extends CXFAuthentication
{
	public static final String NAME = "jaxrs2";
}
