/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.sandbox;

import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;

public class SandboxAuthenticationResult extends RemoteAuthenticationResult
{
	public final SandboxAuthnContext sandboxAuthnInfo;

	public SandboxAuthenticationResult(RemoteAuthenticationResult base, SandboxAuthnContext sandboxAuthnInfo)
	{
		super(base);
		this.sandboxAuthnInfo = sandboxAuthnInfo;
	}
}
