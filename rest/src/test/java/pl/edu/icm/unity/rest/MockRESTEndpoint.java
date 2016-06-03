/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.AuthenticationProcessor;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

public class MockRESTEndpoint extends RESTEndpoint
{
	public MockRESTEndpoint(UnityMessageSource msg, SessionManagement sessionMan, 
			AuthenticationProcessor authnProcessor,
			NetworkServer server, String servletPath)
	{
		super(msg, sessionMan, authnProcessor, server, servletPath);
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
			installExceptionHandlers(ret);
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
}
