/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.engine.api.endpoint.BindingAuthn;

/**
 * Provides information about partial state of authentication. Basing on the contents the 
 * framework should perform additional authentication or proceed to establish final authentication result.
 * @author K. Benedyczak
 */

public class PartialAuthnState
{
	private final String firstFactorOptionId;
	private final BindingAuthn secondaryAuthenticator;
	private final AuthenticationResult primaryResult;
	private final AuthenticationFlow flow;
	
	public PartialAuthnState(String firstFactorOptionId, BindingAuthn secondaryAuthenticator,
			AuthenticationResult result, AuthenticationFlow flow)
	{
		this.firstFactorOptionId = firstFactorOptionId;
		this.secondaryAuthenticator = secondaryAuthenticator;
		this.primaryResult = result;
		this.flow = flow;
	}

	public boolean isSecondaryAuthenticationRequired()
	{
		return secondaryAuthenticator != null;
	}

	public BindingAuthn getSecondaryAuthenticator()
	{
		return secondaryAuthenticator;
	}

	public AuthenticationResult getPrimaryResult()
	{
		return primaryResult;
	}
	
	public AuthenticationFlow getAuthenticationFlow()
	{
		return flow;
	}

	public String getFirstFactorOptionId()
	{
		return firstFactorOptionId;
	}
}