/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote;

import pl.edu.icm.unity.server.authn.SandboxAuthnContext;


/**
 * Stores full information on the remote sandboxed authentication.
 * Either {@link RemotelyAuthenticatedContext} is
 * provided (successful authN) or exception with unprocessed {@link RemotelyAuthenticatedInput}.
 * The most of the information is in authnContext, which is enriched with logs and potential error.
 * User should be careful when using the authnResult. It may happen that many of the 
 * fields are not initialized in case of authentication failure. 
 *  
 * @author K. Benedyczak
 */
public class RemoteSandboxAuthnContext implements SandboxAuthnContext
{
	private RemotelyAuthenticatedContext authnContext;
	private Exception authnException;
	private String logs;

	public RemoteSandboxAuthnContext(RemotelyAuthenticatedContext authnResult, String logs)
	{
		this.authnContext = authnResult;
		this.logs = logs;
	}
	
	public RemoteSandboxAuthnContext(Exception authnException, String logs, 
			RemotelyAuthenticatedInput input)
	{
		this.authnException = authnException;
		this.logs = logs;
		if (input != null)
		{
			authnContext = new RemotelyAuthenticatedContext(input.getIdpName(), null);
			authnContext.setAuthnInput(input);
		}
	}

	public RemotelyAuthenticatedContext getAuthnContext()
	{
		return authnContext;
	}
	public Exception getAuthnException()
	{
		return authnException;
	}
	public String getLogs()
	{
		return logs;
	}
}
