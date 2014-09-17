/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import java.util.EventObject;

import com.vaadin.server.VaadinService;

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
	private StringBuffer capturedLogs;
	private String callerId;
	
	public SandboxAuthnEvent(RemotelyAuthenticatedInput input)
	{
		super(input);
		this.input = input;
		this.callerId = VaadinService.getCurrentRequest().getWrappedSession().getId();
	}

	public SandboxAuthnEvent(AuthenticationResult authnResult, StringBuffer capturedLogs)
	{
		super(authnResult);
		this.authnResult = authnResult;
		this.capturedLogs = capturedLogs;
		this.callerId = VaadinService.getCurrentRequest().getWrappedSession().getId();
	}

	public RemotelyAuthenticatedInput getAuthnInput()
	{
		return input;
	}
	
	public AuthenticationResult getAuthnResult() 
	{
		return authnResult;
	}
	
	public StringBuffer getCapturedLogs()
	{
		return capturedLogs;
	}
	
	public String getCallerId()
	{
		return callerId;
	}

	public String toString()
	{
		return "SandboxAuthnEvent: " + input.getTextDump();
	}
}
