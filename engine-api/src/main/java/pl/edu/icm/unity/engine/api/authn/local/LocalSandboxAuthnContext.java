/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.local;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.SandboxAuthnContext;

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
