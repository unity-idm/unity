/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

import pl.edu.icm.unity.server.authn.remote.SandboxAuthnContext;

import com.vaadin.server.VaadinService;


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
	
	public SandboxAuthnEvent(SandboxAuthnContext ctx)
	{
		this.callerId = VaadinService.getCurrentRequest().getWrappedSession().getId();
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
