/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.local;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;

public interface AccessTokenAndPasswordExchange extends CredentialExchange
{
	public static final String ID = "access token with password exchange";
	
	public AuthenticationResultWithTokenStatus checkToken(BearerAccessToken token) throws AuthenticationException;

	public AuthenticationResult checkPassword(String username, String password) throws AuthenticationException;

}
