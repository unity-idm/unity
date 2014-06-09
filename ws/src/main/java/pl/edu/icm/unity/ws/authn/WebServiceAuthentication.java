/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.authn;

import pl.edu.icm.unity.rest.authn.CXFAuthentication;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;

/**
 * Defines a contract which must be implemented by {@link CredentialRetrieval}s in order to be used 
 * with the {@link CXFEndpoint}.
 * @author K. Benedyczak
 */
public interface WebServiceAuthentication extends CXFAuthentication
{
	public static final String NAME = "webservice-cxf2";
}
