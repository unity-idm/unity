/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;

/**
 * As the result of sandbox authn this event contains {@link RemotelyAuthenticatedInput}. 
 * 
 * @author Roman Krysinski
 *
 */
public class SandboxRemoteAuthnInputEvent extends SandboxAuthnEvent 
{
	private RemotelyAuthenticatedInput input;
	
	public SandboxRemoteAuthnInputEvent(RemotelyAuthenticatedInput input) 
	{
		super();
		this.input = input;
	}
	
	public RemotelyAuthenticatedInput getAuthnInput()
	{
		return input;
	}

	public String toString()
	{
		return this.getClass().getSimpleName() + ": " + input.getTextDump();
	}
}
