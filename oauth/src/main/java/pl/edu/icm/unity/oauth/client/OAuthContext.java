/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.net.URI;

import com.nimbusds.openid.connect.sdk.AuthenticationRequest;

import pl.edu.icm.unity.server.utils.RemoteAuthnState;

/**
 * OAuth specific state associated with one remote login pipeline.
 * <p>
 * This class is thread safe.
 * @author K. Benedyczak
 */
public class OAuthContext extends RemoteAuthnState
{
	private AuthenticationRequest request;
	private URI requestURI;
	private String authzCode;
	private String errorCode;
	private String errorDescription;
	private String returnUrl;
	private String providerConfigKey;

	public void setRequest(AuthenticationRequest request, URI requestURI, String providerConfigKey)
	{
		this.request = request;
		this.requestURI = requestURI;
		this.providerConfigKey = providerConfigKey;
	}

	public synchronized boolean isAnswerPresent()
	{
		return errorCode != null || authzCode != null;
	}
	
	public synchronized String getProviderConfigKey()
	{
		return providerConfigKey;
	}

	public synchronized AuthenticationRequest getRequest()
	{
		return request;
	}

	public synchronized URI getRequestURI()
	{
		return requestURI;
	}

	public synchronized String getAuthzCode()
	{
		return authzCode;
	}

	public synchronized void setAuthzCode(String authzCode)
	{
		this.authzCode = authzCode;
	}

	public synchronized String getReturnUrl()
	{
		return returnUrl;
	}

	public synchronized void setReturnUrl(String returnUrl)
	{
		this.returnUrl = returnUrl;
	}

	public synchronized String getErrorCode()
	{
		return errorCode;
	}

	public synchronized void setErrorCode(String errorCode)
	{
		this.errorCode = errorCode;
	}

	public synchronized String getErrorDescription()
	{
		return errorDescription;
	}

	public synchronized void setErrorDescription(String errorDescription)
	{
		this.errorDescription = errorDescription;
	}
}
