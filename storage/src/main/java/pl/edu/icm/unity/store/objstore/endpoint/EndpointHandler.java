/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;
import pl.edu.icm.unity.types.endpoint.Endpoint;

/**
 * Handler for {@link Endpoint}
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
		return new GenericObjectBean(value.getName(), JsonUtil.serialize2Bytes(value.toJson()), 
				supportedType);
	}

	@Override
	public Endpoint fromBlob(GenericObjectBean blob)
	{
		return new Endpoint(JsonUtil.parse(blob.getContents()));
	}
}
