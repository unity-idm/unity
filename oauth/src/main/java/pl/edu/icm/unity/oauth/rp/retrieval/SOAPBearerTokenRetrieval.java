/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.retrieval;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.oauth.rp.AccessTokenExchange;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;

/**
 * Credential retrieval using OAuth access token as provided with the Bearer Authorization header (RFC 6750).
 * <p>
 * Real implementation is in {@link BearerRetrievalBase}, here only binding is reported.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class SOAPBearerTokenRetrieval extends BearerRetrievalBase implements CredentialRetrieval, JAXRSAuthentication
{
	public static final String NAME = "cxf-oauth-bearer";
	public static final String DESC = "CXFBearerTokenRetrievalFactory.desc";

	public SOAPBearerTokenRetrieval()
	{
		super(WebServiceAuthentication.NAME);
	}
	
	@Component
	public static class Factory extends AbstractCredentialRetrievalFactory<SOAPBearerTokenRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<SOAPBearerTokenRetrieval> factory)
		{
			super(NAME, DESC, WebServiceAuthentication.NAME, factory, AccessTokenExchange.ID);
		}
	}
}
