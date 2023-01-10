/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.rest;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
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
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Collections;
import java.util.List;
import java.util.Set;


@PrototypeComponent
public class RESTUpmanEndpoint extends RESTEndpoint
{
	public static final String NAME = "RESTUpman";
	public static final String V1_PATH = "/v1";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "A RESTful endpoint exposing Unity management API.",
			JAXRSAuthentication.NAME,
			Collections.singletonMap(V1_PATH, "The REST management base path")
		);

	private final ObjectFactory<List<RESTUpman>> factories;

	@Autowired
	public RESTUpmanEndpoint(MessageSource msg,
	                         SessionManagement sessionMan,
	                         NetworkServer server,
	                         AuthenticationProcessor authnProcessor,
	                         ObjectFactory<List<RESTUpman>> factories,
	                         AdvertisedAddressProvider advertisedAddrProvider,
	                         EntityManagement entityMan)
	{
		super(msg, sessionMan, authnProcessor, server, advertisedAddrProvider, "", entityMan);
		this.factories = factories;
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
			Set<Object> ret = Sets.newHashSet(factories.getObject());
			RestEndpointHelper.installExceptionHandlers(ret);
			return ret;
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
