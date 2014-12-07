/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.verificator;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

/**
 * Defines a contract of a token verification implementation, which is AS server implementation dependent.
 * @author K. Benedyczak
 */
public interface TokenVerificatorProtocol
{
	/**
	 * Checks if the provided token is valid in a protocol dependent way.
	 * @param token
	 * @return
	 * @throws Exception 
	 */
	TokenStatus checkToken(BearerAccessToken token) throws Exception;
}
