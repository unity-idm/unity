/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.mock;

import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagementEE8;
import pl.edu.icm.unity.ws.CXFEndpoint;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;

@Component
public class MockWSEndpointFactory implements EndpointFactory
{
	public static final String SERVLET_PATH = "/mock-ws";
	public static final String NAME = "Mock WS Endpoint";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "This is mock web service endpoint for tests", 
			WebServiceAuthentication.NAME, Collections.singletonMap(SERVLET_PATH, "Test endpoint"));

	@Autowired
	private MessageSource msg;
	
	@Autowired
	private SessionManagementEE8 sessionMan;
	
	@Autowired
	private AuthenticationProcessor authnProcessor;
	
	@Autowired
	private NetworkServer server;
	
	@Autowired
	private AdvertisedAddressProvider advertisedAddrProvider;
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return TYPE;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new CXFEndpoint(msg, sessionMan, authnProcessor, server, advertisedAddrProvider, SERVLET_PATH, 
				mock(EntityManagement.class))
		{
			@Override
			protected void configureServices()
			{
				addWebservice(MockWSSEI.class, new MockWSImpl());				
			}
		};
	}
}
