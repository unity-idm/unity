/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.AuthenticationProcessor;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

@Component
public class MockRESTEndpointFactory implements EndpointFactory
{
	public static final String SERVLET_PATH = "/mock-rest";
	public static final String NAME = "Mock REST Endpoint";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "This is mock RESTful endpoint for tests", 
			Collections.singleton(JAXRSAuthentication.NAME),
			Collections.singletonMap(SERVLET_PATH, "Test endpoint"));
	@Autowired
	private UnityMessageSource msg;
	
	@Autowired
	private SessionManagement sessionMan;
	
	@Autowired
	private AuthenticationProcessor authnProcessor;
	
	@Autowired
	private NetworkServer server;
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return TYPE;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new MockRESTEndpoint(msg, sessionMan, authnProcessor, server, SERVLET_PATH);
	}

}
