/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote;

import pl.edu.icm.unity.server.authn.AbstractVerificator;
import pl.edu.icm.unity.server.authn.CredentialExchange;

/**
 * Base class that is nearly mandatory for all remote verificators. The remote verificator should extend it 
 * by implementing a {@link CredentialExchange} of choice. The implementation should obtain the 
 * {@link RemotelyAuthenticatedContext} (the actual coding should be done here) and before returning it should
 * be processed by {@link #processRemoteInput(RemotelyAuthenticatedInput)} to obtain a final 
 * {@link RemotelyAuthenticatedContext}.
 * 
 * @author K. Benedyczak
 */
public abstract class AbstractRemoteVerificator extends AbstractVerificator
{
	public AbstractRemoteVerificator(String name, String description, String exchangeId)
	{
		super(name, description, exchangeId);
	}

	protected final RemotelyAuthenticatedContext processRemoteInput(RemotelyAuthenticatedInput input)
	{
		//TODO
		throw new RuntimeException("Not implemented");
	}
}
