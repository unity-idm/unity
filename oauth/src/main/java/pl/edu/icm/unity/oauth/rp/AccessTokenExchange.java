/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;

/**
 * Interface for validation of an access token obtained by a credential retrieval.
 */
public interface AccessTokenExchange extends CredentialExchange
{
	String ID = "access token exchange";
	
	AuthenticationResult checkToken(BearerAccessToken token) throws AuthenticationException;
}
