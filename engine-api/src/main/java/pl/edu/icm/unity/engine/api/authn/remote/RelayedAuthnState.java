/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import java.util.Date;
import java.util.UUID;

/**
 * Base class for storing some context information related to external login using relay state.
 */
public class RelayedAuthnState
{
	private final String relayState;
	private final Date creationTime;
	
	public RelayedAuthnState()
	{
		this(UUID.randomUUID().toString(), new Date());
	}

	protected RelayedAuthnState(String relayState, Date creationTime)
	{
		this.relayState = relayState;
		this.creationTime = creationTime;
	}

	public String getRelayState()
	{
		return relayState;
	}

	public Date getCreationTime()
	{
		return creationTime;
	}
}