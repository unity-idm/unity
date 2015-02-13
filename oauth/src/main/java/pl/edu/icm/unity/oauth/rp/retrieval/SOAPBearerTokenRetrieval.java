/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.retrieval;

import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;

/**
 * Credential retrieval using OAuth access token as provided with the Bearer Authorization header (RFC 6750).
 * <p>
 * Real implementation is in {@link BearerRetrievalBase}, here only binding is reported.
 * @author K. Benedyczak
 */
public class SOAPBearerTokenRetrieval extends BearerRetrievalBase implements CredentialRetrieval, JAXRSAuthentication
{
	public SOAPBearerTokenRetrieval()
	{
		super(WebServiceAuthentication.NAME);
	}
}
