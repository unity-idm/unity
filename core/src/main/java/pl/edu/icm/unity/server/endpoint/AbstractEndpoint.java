/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.endpoint;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

/**
 * Typical boilerplate for all endpoints.
 * @author K. Benedyczak
 */
public abstract class AbstractEndpoint implements EndpointInstance
{
	protected EndpointDescription description;
	protected List<AuthenticationOption> authenticators;
	protected Properties properties;
	
	@Override
	public synchronized void initialize(EndpointDescription description, 
			List<AuthenticationOption> authenticators,
			String serializedConfiguration)
	{
		this.description = description;
		this.authenticators = authenticators;
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
	public EndpointDescription getEndpointDescription()
	{
		return description;
	}

	@Override
	public void destroy() throws EngineException
	{
	}
	
	@Override
	public synchronized List<AuthenticationOption> getAuthenticationOptions()
	{
		return authenticators;
	}

	protected synchronized void setAuthenticators(List<AuthenticationOption> authenticators)
	{
		this.authenticators = new ArrayList<>(authenticators);
	}
}
