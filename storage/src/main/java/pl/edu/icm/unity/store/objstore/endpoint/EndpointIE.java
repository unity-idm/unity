/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.store.api.generic.EndpointDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;

/**
 * Handles import/export of {@link Endpoint}.
 * @author K. Benedyczak
 */
@Component
public class EndpointIE extends GenericObjectIEBase<Endpoint>
{
	@Autowired
	public EndpointIE(EndpointDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, 111, EndpointHandler.ENDPOINT_OBJECT_TYPE);
	}

	@Override
	protected Endpoint convert(ObjectNode src)
	{
		return EndpointMapper.map(jsonMapper.convertValue(src, DBEndpoint.class));
	}

	@Override
	protected ObjectNode convert(Endpoint src)
	{
		return jsonMapper.convertValue(EndpointMapper.map(src), ObjectNode.class);
	}
}



