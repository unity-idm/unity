/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;

/**
 * Thrown upon an error which was translated into a final OAuth error response.
 * The handling code must only send the response back using an appropriate mechanism.
 * @author K. Benedyczak
 */
public class OAuthErrorResponseException extends Exception
{
	private AuthorizationErrorResponse oauthResponse;
	private boolean invalidateSession;
	
	public OAuthErrorResponseException(AuthorizationErrorResponse oauthResponse,
			boolean invalidateSession)
	{
		this.oauthResponse = oauthResponse;
		this.invalidateSession = invalidateSession;
	}

	public AuthorizationErrorResponse getOauthResponse()
	{
		return oauthResponse;
	}

	public boolean isInvalidateSession()
	{
		return invalidateSession;
	}
}
