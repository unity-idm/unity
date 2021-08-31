/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import java.util.Optional;

import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnContext;


/**
 * Stores full information on the remote sandboxed authentication.
 * Either {@link RemotelyAuthenticatedPrincipal} is
 * provided (successful authN) or exception with unprocessed {@link RemotelyAuthenticatedInput}.
 * The most of the information is in authnContext, which is enriched with logs and potential error.
 * User should be careful when using the authnResult. It may happen that many of the 
 * fields are not initialized in case of authentication failure. 
 *  
 * @author K. Benedyczak
 */
public class RemoteSandboxAuthnContext implements SandboxAuthnContext
{
	private final Optional<RemotelyAuthenticatedPrincipal> authnContext;
	private final Optional<Exception> authnException;
	private final String logs;

	private RemoteSandboxAuthnContext(RemotelyAuthenticatedPrincipal authnContext, Exception authnException,
			String logs)
	{
		this.authnContext = Optional.ofNullable(authnContext);
		this.authnException = Optional.ofNullable(authnException);
		this.logs = logs;
	}

	public static RemoteSandboxAuthnContext succeededAuthn(RemotelyAuthenticatedPrincipal authnResult, String logs)
	{
		return new RemoteSandboxAuthnContext(authnResult, null, logs);
	}
	
	public static RemoteSandboxAuthnContext failedAuthn(Exception authnException, String logs, 
			RemotelyAuthenticatedInput input)
	{
		RemotelyAuthenticatedPrincipal authnContext;
		if (input != null)
		{
			authnContext = new RemotelyAuthenticatedPrincipal(input.getIdpName(), null);
			authnContext.setAuthnInput(input);
		} else
			authnContext = null;
		return new RemoteSandboxAuthnContext(authnContext, authnException, logs);
	}

	@Override
	public Optional<RemotelyAuthenticatedPrincipal> getRemotePrincipal()
	{
		return authnContext;
	}
	
	@Override
	public Optional<Exception> getAuthnException()
	{
		return authnException;
	}
	
	@Override
	public String getLogs()
	{
		return logs;
	}

	@Override
	public String toString()
	{
		return String.format("RemoteSandboxAuthnContext [authnContext=%s, authnException=%s]",
				authnContext, authnException);
	}
}
