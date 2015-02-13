/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.authn.ext;

import pl.edu.icm.unity.rest.authn.ext.HttpBasicRetrievalBase;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;

/**
 * Credential retrieval using username and password from the HTTP Basic Authn.
 * Here we only return a proper name, implementation in parent.
 * @author K. Benedyczak
 */
public class HttpBasicRetrieval extends HttpBasicRetrievalBase implements CredentialRetrieval, WebServiceAuthentication
{
	public HttpBasicRetrieval()
	{
		super(WebServiceAuthentication.NAME);
	}
}
