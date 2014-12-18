/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import java.util.Date;
import java.util.UUID;

import pl.edu.icm.unity.server.authn.remote.SandboxAuthnResultCallback;

/**
 * Base class for storing some context information related to external login.
 * @author K. Benedyczak
 */
public class RemoteAuthnState
{
	private String relayState;
	private Date creationTime;
	private SandboxAuthnResultCallback sandboxCallback;
	
	public RemoteAuthnState()
	{
		this.relayState = UUID.randomUUID().toString();
		this.creationTime = new Date();
	}

	public String getRelayState()
	{
		return relayState;
	}

	public Date getCreationTime()
	{
		return creationTime;
	}

	public SandboxAuthnResultCallback getSandboxCallback()
	{
		return sandboxCallback;
	}

	public void setSandboxCallback(SandboxAuthnResultCallback sandboxCallback)
	{
		this.sandboxCallback = sandboxCallback;
	}
}