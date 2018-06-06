/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationOption;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;

/**
 * No-op implementation of sandbox authN processor.
 * 
 * TODO refactor to implement an interface
 * @author K. Benedyczak
 */
public class SandboxAuthenticationProcessor extends WebAuthenticationProcessor
{
	@Override
	public PartialAuthnState processPrimaryAuthnResult(AuthenticationResult result, String clientIp, 
			AuthenticationRealm realm,
			AuthenticationOption authenticationOption, boolean rememberMe) throws AuthenticationException
	{
		return null;
	}

	@Override
	public void processSecondaryAuthnResult(PartialAuthnState state, AuthenticationResult result2, String clientIp, 
			AuthenticationRealm realm,
			AuthenticationOption authenticationOption, boolean rememberMe) throws AuthenticationException
	{
		throw new IllegalStateException("This method should never be called in sandboxed authN");
	}
	
	@Override
	public void logout()
	{
	}

	@Override
	public void logout(boolean soft)
	{
	}
}
