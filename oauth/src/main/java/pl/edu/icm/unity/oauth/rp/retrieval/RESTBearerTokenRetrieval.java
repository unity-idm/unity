/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.retrieval;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.oauth.rp.AccessTokenExchange;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;

/**
 * Credential retrieval using OAuth access token as provided with the Bearer Authorization header (RFC 6750).
 * <p>
 * Real implementation is in {@link BearerRetrievalBase}, here only binding is reported.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class RESTBearerTokenRetrieval extends BearerRetrievalBase implements JAXRSAuthentication
{
	public static final String NAME = "rest-oauth-bearer";
	public static final String DESC = "RESTBearerTokenRetrievalFactory.desc";
	
	public RESTBearerTokenRetrieval()
	{
		super(JAXRSAuthentication.NAME);
	}
	
	@Component
	public static class Factory extends AbstractCredentialRetrievalFactory<RESTBearerTokenRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<RESTBearerTokenRetrieval> factory)
		{
			super(NAME, DESC, JAXRSAuthentication.NAME, factory, AccessTokenExchange.ID);
		}
	}
}
