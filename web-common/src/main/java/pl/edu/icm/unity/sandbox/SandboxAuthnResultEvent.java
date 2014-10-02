/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import pl.edu.icm.unity.server.authn.AuthenticationResult;

/**
 * As the result of sandbox authn this event contains {@link AuthenticationResult}
 * and logs recorded during authentication and profile evaluation. 
 * 
 * @author Roman Krysinski
 *
 */
public class SandboxAuthnResultEvent extends SandboxAuthnEvent 
{
	private AuthenticationResult authnResult;
	private StringBuffer capturedLogs;
	
	public SandboxAuthnResultEvent(AuthenticationResult authnResult,
			StringBuffer capturedLogs) 
	{
		super();
		this.authnResult = authnResult;
		this.capturedLogs = capturedLogs;		
	}

	public AuthenticationResult getAuthnResult() 
	{
		return authnResult;
	}
	
	public StringBuffer getCapturedLogs()
	{
		return capturedLogs;
	}
}
