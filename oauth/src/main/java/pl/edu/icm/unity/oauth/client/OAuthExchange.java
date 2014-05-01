/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.CredentialExchange;

/**
 * Defines comunication of a OAuth verificator and retrieval.
 * @author K. Benedyczak
 */
public interface OAuthExchange extends CredentialExchange
{
	public static final String ID = "OAuth2 exchange";
	
	OAuthClientProperties getSettings();
	OAuthClientRequest createRequest() throws OAuthSystemException;
	AuthenticationResult verifyOAuthAuthzResponse(OAuthAuthzResponse oar);
}
