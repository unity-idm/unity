/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox;

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
	
	public SandboxAuthnEvent()
	{
		this.callerId = VaadinService.getCurrentRequest().getWrappedSession().getId();
	}

	public String getCallerId()
	{
		return callerId;
	}
}
