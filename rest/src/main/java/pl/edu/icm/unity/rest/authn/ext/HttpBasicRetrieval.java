/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn.ext;

import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;

/**
 * Credential retrieval using username and password from the HTTP Basic Authn.
 * <p>
 * Real implementation in parent class, here we only report a unique name.
 * @author K. Benedyczak
 */
public class HttpBasicRetrieval extends HttpBasicRetrievalBase implements CredentialRetrieval, JAXRSAuthentication
{
	@Override
	public String getBindingName()
	{
		return JAXRSAuthentication.NAME;
	}
}
