/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest;

import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;

@PrototypeComponent
public class MockRESTEndpoint extends RESTEndpoint
{
	public static final String SERVLET_PATH = "/mock-rest";
	public static final String NAME = "Mock REST Endpoint";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "This is mock RESTful endpoint for tests", 
			JAXRSAuthentication.NAME,
			Collections.singletonMap(SERVLET_PATH, "Test endpoint"));

	@Autowired
	public MockRESTEndpoint(MessageSource msg,
			SessionManagement sessionMan,
			AuthenticationProcessor authnProcessor,
			NetworkServer server,
			AdvertisedAddressProvider advertisedAddrProvider)
	{
		super(msg, sessionMan, authnProcessor, server, advertisedAddrProvider, SERVLET_PATH, mock(EntityManagement.class));
	}


	@Override
	protected Application getApplication()
	{
		return new JAXRSProvider();
	}

	@ApplicationPath("/test")
	public static class JAXRSProvider extends Application
	{
		private MockResource res = new MockResource();
		
		@Override 
		public Set<Object> getSingletons() 
		{
			HashSet<Object> ret = new HashSet<>();
			ret.add(res);
			RestEndpointHelper.installExceptionHandlers(ret);
			return ret;
		}
	}
	
	@Path("/r1/")
	@Produces("text/plain")
	public static class MockResource
	{
		@GET
		public String getDate() 
		{
			return new Date().toString();
		}
		
		@GET
		@Path("exception")
		public String getError() throws AuthorizationException 
		{
			throw new AuthorizationException("Test exception");
		}
	}
	
	@Component
	public static class Factory implements EndpointFactory
	{
		@Autowired
		private ObjectFactory<MockRESTEndpoint> factory;
		
		@Override
		public EndpointTypeDescription getDescription()
		{
			return TYPE;
		}

		@Override
		public EndpointInstance newInstance()
		{
			return factory.getObject();
		}
	}

}
