/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.mock;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.ws.CXFEndpoint;
import pl.edu.icm.unity.ws.authn.CXFAuthentication;

@Component
public class MockWSEndpointFactory implements EndpointFactory
{
	public static final String SERVLET_PATH = "/mock-ws";
	public static final String NAME = "Mock WS Endpoint";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "This is mock web service endpoint for tests", 
			Collections.singleton(CXFAuthentication.NAME),Collections.singletonMap(SERVLET_PATH, "Test endpoint"));

	@Autowired
	private UnityMessageSource msg;
	
	@Autowired
	private SessionManagement sessionMan;
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return TYPE;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new CXFEndpoint(msg, sessionMan, getDescription(), SERVLET_PATH)
		{
			@Override
			protected void configureServices()
			{
				addWebservice(MockWSSEI.class, new MockWSImpl());				
			}
		};
	}
}
