/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.objhandlers;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.internal.InternalEndpointManagement;
import pl.edu.icm.unity.server.utils.DefaultGenericObjectHandler;

@Component
public class EndpointObjectHandler extends DefaultGenericObjectHandler
{
	public EndpointObjectHandler()
	{
		super(InternalEndpointManagement.ENDPOINT_OBJECT_TYPE);
	}
}
