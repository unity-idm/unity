/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;

import pl.edu.icm.unity.server.utils.RemoteAuthnState;

/**
 * OAuth specific state associated with one remote login pipeline.
 * @author K. Benedyczak
 */
public class OAuthContext extends RemoteAuthnState
{
	private OAuthClientRequest request;
	private OAuthAuthzResponse authzResponse;
	private OAuthProblemException error;
	private String registrationFormForUnknown;
	private String returnUrl;
	private String clientId;
	private String clientSecret;
	private String tokenEndpoint;
	private String profileEndpoint;
	private boolean nonJsonMode;

	public OAuthClientRequest getRequest()
	{
		return request;
	}

	public void setRequest(OAuthClientRequest request, String registrationFormForUnknown, String clientId,
			String clientSecret, String tokenEndpoint, String profileEndpoint, boolean nonJsonMode)
	{
		this.request = request;
		this.registrationFormForUnknown = registrationFormForUnknown;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.tokenEndpoint = tokenEndpoint;
		this.nonJsonMode = nonJsonMode;
		this.profileEndpoint = profileEndpoint;
	}

	public OAuthAuthzResponse getAuthzResponse()
	{
		return authzResponse;
	}

	public void setAuthzResponse(OAuthAuthzResponse authzResponse)
	{
		this.authzResponse = authzResponse;
	}

	public String getRegistrationFormForUnknown()
	{
		return registrationFormForUnknown;
	}

	public String getReturnUrl()
	{
		return returnUrl;
	}

	public void setReturnUrl(String returnUrl)
	{
		this.returnUrl = returnUrl;
	}

	public String getClientId()
	{
		return clientId;
	}

	public String getClientSecret()
	{
		return clientSecret;
	}

	public String getTokenEndpoint()
	{
		return tokenEndpoint;
	}

	public boolean isNonJsonMode()
	{
		return nonJsonMode;
	}

	public OAuthProblemException getError()
	{
		return error;
	}

	public void setError(OAuthProblemException error)
	{
		this.error = error;
	}

	public String getProfileEndpoint()
	{
		return profileEndpoint;
	}
}
