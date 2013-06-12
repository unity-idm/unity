/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.mock;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
			Collections.singleton(CXFAuthentication.NAME));

	@Autowired
	private UnityMessageSource msg;
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return TYPE;
	}

	@Override
	public EndpointInstance newInstance()
	{
		CXFEndpoint endpoint = new CXFEndpoint(msg, getDescription(), SERVLET_PATH);
		endpoint.addWebservice(MockWSSEI.class, new MockWSImpl());
		return endpoint;
	}
}
