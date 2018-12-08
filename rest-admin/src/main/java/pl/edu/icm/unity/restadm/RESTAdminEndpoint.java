/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.rest.RESTEndpoint;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * RESTful endpoint providing administration and query API.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class RESTAdminEndpoint extends RESTEndpoint
{
	public static final String NAME = "RESTAdmin";
	public static final String V1_PATH = "/v1";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "A RESTful endpoint exposing Unity management API.", 
			JAXRSAuthentication.NAME,
			Collections.singletonMap(V1_PATH, "The REST management base path"));
	
	private ObjectFactory<RESTAdmin> factory;
	
	@Autowired
	public RESTAdminEndpoint(UnityMessageSource msg, SessionManagement sessionMan,
			NetworkServer server, AuthenticationProcessor authnProcessor, 
			ObjectFactory<RESTAdmin> factory)
	{
		super(msg, sessionMan, authnProcessor, server, "");
		this.factory = factory;
	}

	@Override
	protected Application getApplication()
	{
		return new RESTAdminJAXRSApp();
	}

	@ApplicationPath("/")
	public class RESTAdminJAXRSApp extends Application
	{
		@Override 
		public Set<Object> getSingletons() 
		{
			HashSet<Object> ret = new HashSet<>();
			ret.add(factory.getObject());
			installExceptionHandlers(ret);
			return ret;
		}
	}
	
	@Component
	public static class Factory implements EndpointFactory
	{
		@Autowired
		private ObjectFactory<RESTAdminEndpoint> factory;
		
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
