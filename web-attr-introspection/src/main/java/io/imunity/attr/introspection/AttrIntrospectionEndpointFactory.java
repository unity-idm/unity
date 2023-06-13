/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection;

import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.endpoint.common.InsecureVaadin2XEndpoint;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;

import java.util.Collections;

@Component
public class AttrIntrospectionEndpointFactory implements EndpointFactory
{
	public static final String NAME = "AttributeIntrospection";
	public static final String SERVLET_PATH = "/introspection";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(NAME,
			"Attribute introspection endpoint", VaadinAuthentication.NAME,
			Collections.singletonMap(SERVLET_PATH, "Attribute introspection endpoint"));

	private final ApplicationContext applicationContext;
	private final NetworkServer server;
	private final MessageSource msg;
	private final AdvertisedAddressProvider advertisedAddrProvider;
	private final RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter;
	private final ObjectFactory<SandboxAuthnRouter> sandboxAuthnRouterFactory;

	@Autowired
	AttrIntrospectionEndpointFactory(ApplicationContext applicationContext, NetworkServer server,
									 AdvertisedAddressProvider advertisedAddrProvider, MessageSource msg,
									 RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter,
									 ObjectFactory<SandboxAuthnRouter> sandboxAuthnRouterFactory)
	{
		this.applicationContext = applicationContext;
		this.server = server;
		this.msg = msg;
		this.advertisedAddrProvider = advertisedAddrProvider;
		this.remoteAuthnResponseProcessingFilter = remoteAuthnResponseProcessingFilter;
		this.sandboxAuthnRouterFactory = sandboxAuthnRouterFactory;
	}

	@Override
	public EndpointTypeDescription getDescription()
	{
		return TYPE;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new InsecureVaadin2XEndpoint(server, advertisedAddrProvider, msg, applicationContext,
				new AttrIntrospectionResourceProvider(), SERVLET_PATH, remoteAuthnResponseProcessingFilter,
				sandboxAuthnRouterFactory.getObject(), AttrIntrospectionServlet.class);
	}
}
