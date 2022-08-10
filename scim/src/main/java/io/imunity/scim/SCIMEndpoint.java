/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.scim.config.SCIMEndpointConfiguration;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.config.SCIMEndpointPropertiesConfigurationMapper;
import io.imunity.scim.exception.providers.SCIMEndpointExceptionMapper;
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

	private final List<SCIMRestControllerFactory> factories;
	protected SCIMEndpointConfiguration scimEndpointConfiguration;
	private final MembershipGroupsProvider membershipGroupProvider;

	@Autowired
	public SCIMEndpoint(MessageSource msg, SessionManagement sessionMan, NetworkServer server,
			AuthenticationProcessor authnProcessor, List<SCIMRestControllerFactory> factories,
			AdvertisedAddressProvider advertisedAddrProvider, EntityManagement entityMan,
			MembershipGroupsProvider membershipGroupProvider)
	{
		super(msg, sessionMan, authnProcessor, server, advertisedAddrProvider, "", entityMan);
		this.factories = factories;
		this.membershipGroupProvider = membershipGroupProvider;
	}

	@Override
	protected void setSerializedConfiguration(String serializedState)
	{
		super.setSerializedConfiguration(serializedState);
		scimEndpointConfiguration = SCIMEndpointPropertiesConfigurationMapper.fromProperties(serializedState);
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
					scimEndpointConfiguration.rootGroup, membershipGroupProvider.getEffectiveMembershipGroups(scimEndpointConfiguration),
					scimEndpointConfiguration.schemas, scimEndpointConfiguration.membershipAttributes,
					getEndpointDescription().getEndpoint().getConfiguration().getAuthenticationOptions());
			Set<Object> ret = factories.stream().map(f -> f.getController(enDesc)).collect(Collectors.toSet());
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
