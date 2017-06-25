/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.endpoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.EndpointDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.endpoint.Endpoint;

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
		super(dao, jsonMapper, Endpoint.class, 111, EndpointHandler.ENDPOINT_OBJECT_TYPE);
	}
}



