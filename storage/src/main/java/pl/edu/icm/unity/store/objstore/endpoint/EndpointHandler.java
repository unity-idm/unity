/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;
import pl.edu.icm.unity.types.endpoint.Endpoint;

/**
 * Handler for {@link Endpoint}
 * 
 * @author K. Benedyczak
 */
@Component
public class EndpointHandler extends DefaultEntityHandler<Endpoint>
{
	public static final String ENDPOINT_OBJECT_TYPE = "endpointDefinition";

	@Autowired
	public EndpointHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, ENDPOINT_OBJECT_TYPE, Endpoint.class);
	}

	@Override
	public GenericObjectBean toBlob(Endpoint value)
	{
		try
		{
			return new GenericObjectBean(value.getName(), jsonMapper.writeValueAsBytes(EndpointMapper.map(value)),
					supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize endpoint to JSON", e);
		}
	}

	@Override
	public Endpoint fromBlob(GenericObjectBean blob)
	{
		try
		{
			return EndpointMapper.map(jsonMapper.readValue(blob.getContents(), DBEndpoint.class));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize endpoint from JSON", e);
		}
	}
}
