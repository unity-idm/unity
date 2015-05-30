/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

/**
 * Result of sandbox authn using a local facility. Wrapper of {@link AuthenticationResult}. 
 * @author K. Benedyczak
 */
public class LocalSandboxAuthnContext implements SandboxAuthnContext
{
	private AuthenticationResult authenticationResult;

	public LocalSandboxAuthnContext(AuthenticationResult authenticationResult)
	{
		this.authenticationResult = authenticationResult;
	}

	public AuthenticationResult getAuthenticationResult()
	{
		return authenticationResult;
	}
}
