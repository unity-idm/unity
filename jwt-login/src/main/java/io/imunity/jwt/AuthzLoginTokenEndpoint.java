/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.jwt.AuthzLoginTokenController.JWTAuthenticationControllerFactory;
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
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

@PrototypeComponent
public class AuthzLoginTokenEndpoint extends RESTEndpoint
{
	public static final String NAME = "JWTAuthzLoginToken";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "A RESTful endpoint allowing to initiate JWT login process", 
			JAXRSAuthentication.NAME,
			Collections.singletonMap("", "The JWT authz login token base path"));
	
	private final JWTAuthenticationControllerFactory jwtAuthenticationControllerFactory;

	@Autowired
	public AuthzLoginTokenEndpoint(MessageSource msg,
			SessionManagement sessionMan,
			AuthenticationProcessor authenticationProcessor,
			NetworkServer networkServer,
			AdvertisedAddressProvider advertisedAddrProvider,
			EntityManagement identitiesMan,
			JWTAuthenticationControllerFactory jwtAuthenticationControllerFactory)
	{
		super(msg, sessionMan, authenticationProcessor, networkServer, advertisedAddrProvider, "", identitiesMan);
		this.jwtAuthenticationControllerFactory = jwtAuthenticationControllerFactory;
	}

	@Override
	protected Application getApplication()
	{
		return new JWTAuthenticationJAXRSApp(jwtAuthenticationControllerFactory.get(getSerializedConfiguration()));
	}

	@ApplicationPath("/")
	public static class JWTAuthenticationJAXRSApp extends Application
	{
		private AuthzLoginTokenController engine;
		
		public JWTAuthenticationJAXRSApp(AuthzLoginTokenController engine)
		{
			this.engine = engine;
		}

		@Override
		public Set<Class<?>> getClasses()
		{
			return Set.of(UnityJacksonJaxbJsonProvider.class);
		}

		@Override 
		public Set<Object> getSingletons() 
		{
			HashSet<Object> ret = new HashSet<>();
			ret.add(engine);
			return ret;
		}
	}
	
	@Component
	public static class Factory implements EndpointFactory
	{
		private final ObjectFactory<AuthzLoginTokenEndpoint> factory;
		
		public Factory(ObjectFactory<AuthzLoginTokenEndpoint> factory)
		{
			this.factory = factory;
		}

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
