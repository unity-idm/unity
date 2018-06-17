/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.endpoint;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

/**
 * Typical boilerplate for all endpoints.
 * @author K. Benedyczak
 */
public abstract class AbstractEndpoint implements EndpointInstance
{
	protected ResolvedEndpoint description;
	protected List<AuthenticationFlow> authenticationFlows;
	protected Properties properties;
	
	@Override
	public synchronized void initialize(ResolvedEndpoint description, 
			List<AuthenticationFlow> authenticators,
			String serializedConfiguration)
	{
		this.description = description;
		this.authenticationFlows = authenticators;
		setSerializedConfiguration(serializedConfiguration);
	}

	@Override
	public String getSerializedConfiguration()
	{
		CharArrayWriter writer = new CharArrayWriter();
		try
		{
			properties.store(writer, "");
		} catch (IOException e)
		{
			throw new IllegalStateException("Can not serialize endpoint's configuration", e);
		}
		return writer.toString();
	}
	
	protected abstract void setSerializedConfiguration(String serializedState);
	
	@Override
	public ResolvedEndpoint getEndpointDescription()
	{
		return description;
	}

	@Override
	public void destroy() throws EngineException
	{
		for (AuthenticationFlow ao: authenticationFlows)
			ao.destroy();
	}
	
	@Override
	public synchronized List<AuthenticationFlow> getAuthenticationFlows()
	{
		return authenticationFlows;
	}

	protected synchronized void setAuthenticators(List<AuthenticationFlow> authenticationFlows)
	{
		this.authenticationFlows = new ArrayList<>(authenticationFlows);
	}
}
