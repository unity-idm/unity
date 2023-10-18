/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.rest;

import org.springframework.beans.factory.ObjectFactory;
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
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.rest.RESTEndpoint;
import pl.edu.icm.unity.rest.RestEndpointHelper;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


@PrototypeComponent
public class RESTUpmanEndpoint extends RESTEndpoint
{
	public static final String NAME = "RESTUpman";
	public static final String V1_PATH = "/v1";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "A RESTful endpoint exposing Upman management API.",
			JAXRSAuthentication.NAME,
			Collections.singletonMap(V1_PATH, "The REST management base path")
		);

	private final RESTUpmanController.RESTUpmanControllerFactory factory;

	@Autowired
	public RESTUpmanEndpoint(MessageSource msg,
	                         SessionManagement sessionMan,
	                         NetworkServer server,
	                         AuthenticationProcessor authnProcessor,
	                         RESTUpmanController.RESTUpmanControllerFactory factory,
	                         AdvertisedAddressProvider advertisedAddrProvider,
	                         EntityManagement entityMan)
	{
		super(msg, sessionMan, authnProcessor, server, advertisedAddrProvider, "", entityMan);
		this.factory = factory;
	}

	@Override
	protected Application getApplication()
	{
		return new UpmanRESTJAXRSApp();
	}

	@ApplicationPath("/")
	public class UpmanRESTJAXRSApp extends Application
	{
		@Override 
		public Set<Object> getSingletons() 
		{
			Set<Object> objects = new HashSet<>();
			UpmanRestEndpointProperties upmanRestEndpointProperties = new UpmanRestEndpointProperties(properties);
			RESTUpmanController ret =
				factory.newInstance(
					upmanRestEndpointProperties.getValue(UpmanRestEndpointProperties.ROOT_GROUP),
					upmanRestEndpointProperties.getValue(UpmanRestEndpointProperties.AUTHORIZATION_GROUP)
				);
			objects.add(ret);
			RestEndpointHelper.installExceptionHandlers(objects);
			return objects;
		}
	}
	
	@Component
	public static class Factory implements EndpointFactory
	{
		@Autowired
		private ObjectFactory<RESTUpmanEndpoint> factory;
		
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
