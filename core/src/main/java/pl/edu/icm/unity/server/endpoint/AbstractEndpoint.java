/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.endpoint;

import pl.edu.icm.unity.exceptions.IllegalConfigurationDataException;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Typical boilerplate for all endpoints.
 * @author K. Benedyczak
 */
public abstract class AbstractEndpoint implements EndpointInstance
{
	protected EndpointDescription description;
	
	public AbstractEndpoint(EndpointTypeDescription type)
	{
		description = new EndpointDescription();
		description.setType(type);
	}

	@Override
	public void setId(String id)
	{
		description.setId(id);
	}

	@Override
	public EndpointDescription getEndpointDescription()
	{
		return description;
	}


	@Override
	public void setDescription(String description)
	{
		this.description.setDescription(description);
	}

	@Override
	public void destroy()
	{
	}
	
	@Override
	public void configure(String contextAddress, String jsonConfiguration)
			throws IllegalConfigurationDataException
	{
		description.setContextAddress(contextAddress);
	}
}
