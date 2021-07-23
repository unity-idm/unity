/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.sandbox;

/**
 * Event that represents sandbox authentication. The callerId represents the session id
 * of the notifier.
 * 
 * @author Roman Krysinski
 */
public class SandboxAuthnEvent 
{
	private String callerId;
	private SandboxAuthnContext ctx;
	
	public SandboxAuthnEvent(SandboxAuthnContext ctx, String callerId)
	{
		this.callerId = callerId;
		this.ctx = ctx;
	}

	public String getCallerId()
	{
		return callerId;
	}

	public SandboxAuthnContext getCtx()
	{
		return ctx;
	}
}
