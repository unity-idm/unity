/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim;

import java.io.StringReader;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.scim.config.SCIMEndpointConfiguration;
import io.imunity.scim.config.SCIMEndpointConfigurationMapper;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.exceptions.mapper.SCIMEndpointExceptionMapper;
import io.imunity.scim.handlers.SCIMHandlerFactory;
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
public class SCIMEndpoint extends RESTEndpoint
{
	public static final String NAME = "SCIM";
	public static final String PATH = "";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(NAME,
			"A RESTful endpoint exposing SCIM API.", JAXRSAuthentication.NAME,
			Collections.singletonMap(PATH, "The SCIM base path"));

	private final List<SCIMHandlerFactory> factories;
	protected SCIMEndpointConfiguration scimEndpointConfiguration;

	@Autowired
	public SCIMEndpoint(MessageSource msg, SessionManagement sessionMan, NetworkServer server,
			AuthenticationProcessor authnProcessor, List<SCIMHandlerFactory> factories,
			AdvertisedAddressProvider advertisedAddrProvider, EntityManagement entityMan)
	{
		super(msg, sessionMan, authnProcessor, server, advertisedAddrProvider, "", entityMan);
		this.factories = factories;
	}

	@Override
	protected void setSerializedConfiguration(String serializedState)
	{
		properties = new Properties();
		try
		{
			properties.load(new StringReader(serializedState));
			SCIMEndpointProperties scimEndpointProperties = new SCIMEndpointProperties(properties);
			genericEndpointProperties = scimEndpointProperties;
			scimEndpointConfiguration = SCIMEndpointConfigurationMapper.fromProperties(scimEndpointProperties);

		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the scim endpoint's configuration", e);
		}
	}

	@Override
	protected Application getApplication()
	{
		return new SCIMJAXRSApp();
	}

	@ApplicationPath("/")
	public class SCIMJAXRSApp extends Application
	{
		@Override
		public Set<Object> getSingletons()
		{
			SCIMEndpointDescription enDesc = new SCIMEndpointDescription(URI.create(getServletUrl("")),
					scimEndpointConfiguration.rootGroup, scimEndpointConfiguration.membershipGroups);
			Set<Object> ret = factories.stream().map(f -> f.getHandler(enDesc)).collect(Collectors.toSet());
			SCIMEndpointExceptionMapper.installExceptionHandlers(ret);
			return ret;
		}
	}

	@Component
	public static class Factory implements EndpointFactory
	{
		@Autowired
		private ObjectFactory<SCIMEndpoint> factory;

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
