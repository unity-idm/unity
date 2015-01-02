/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import java.lang.reflect.Proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.server.api.EndpointManagement;

/**
 * Java dynamic proxy builder, decorating wrapped objects with event generation: endpoints
 *   
 * @author K. Benedyczak
 */
public class EventProxyBuilderEndpoint
{
	private static final ClassLoader classLoader = EventProxyBuilderEndpoint.class.getClassLoader();
	
	@Autowired @Qualifier("plain")
	private EndpointManagement endpointMan;
	
	@Autowired
	private EventProcessor eventProcessor;

	
	public EndpointManagement getEndpointsManagementInstance()
	{
		return (EndpointManagement) Proxy.newProxyInstance(classLoader, 
				new Class[] {EndpointManagement.class}, 
				new EventDecoratingHandler(endpointMan, eventProcessor, 
						EndpointManagement.class.getSimpleName()));
	}
}
