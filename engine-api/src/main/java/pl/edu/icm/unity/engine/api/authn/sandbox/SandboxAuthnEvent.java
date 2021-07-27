/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.sandbox;

import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;

/**
 * Event that provides details of a finished sandbox authentication.
 * 
 * @author Roman Krysinski
 */
public class SandboxAuthnEvent 
{
	public final String callerId;
	public final SandboxAuthnContext ctx;
	public final AuthenticatedEntity entity;
	
	public SandboxAuthnEvent(SandboxAuthnContext ctx, AuthenticatedEntity entity, String callerId)
	{
		this.entity = entity;
		this.callerId = callerId;
		this.ctx = ctx;
	}

	@Override
	public String toString()
	{
		return String.format("SandboxAuthnEvent [callerId=%s, ctx=%s, entity=%s]", callerId, ctx, entity);
	}
}
