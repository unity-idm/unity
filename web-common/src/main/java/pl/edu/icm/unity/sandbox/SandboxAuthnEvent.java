/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import java.util.EventObject;

import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;

/**
 * Event that represents sandbox authentication. As the result of sanity authn
 * this event contains {@link RemotelyAuthenticatedInput}.  
 * 
 * @author R Krysinski
 */
public class SandboxAuthnEvent extends EventObject 
{
	private static final long serialVersionUID = 9068192207497978728L;
	private RemotelyAuthenticatedInput input;
	private AuthenticationResult authnResult;
	
	public SandboxAuthnEvent(RemotelyAuthenticatedInput input)
	{
		super(input);
		this.input = input;
	}

	public SandboxAuthnEvent(AuthenticationResult authnResult)
	{
		super(authnResult);
		this.authnResult = authnResult;
	}

	public RemotelyAuthenticatedInput getAuthnInput()
	{
		return input;
	}
	
	public AuthenticationResult getAuthnResult() 
	{
		return authnResult;
	}

	public String toString()
	{
		return "SandboxAuthnEvent: " + input.getTextDump();
	}
}
