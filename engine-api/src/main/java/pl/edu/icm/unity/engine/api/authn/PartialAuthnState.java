/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
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
	private BindingAuthn secondaryAuthenticator;
	private AuthenticationResult primaryResult;

	public PartialAuthnState(BindingAuthn secondaryAuthenticator,
			AuthenticationResult result)
	{
		this.secondaryAuthenticator = secondaryAuthenticator;
		this.primaryResult = result;
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
}