/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.console;

import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.auth.server.SecureVaadin2XEndpoint;
import io.imunity.vaadin.endpoint.common.RemoteRedirectedAuthnResponseProcessingFilter;
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

import java.util.Collections;

@Component
public class ConsoleEndpointFactory implements EndpointFactory
{
	public static final String NAME = "ConsoleUI";
	public static final String SERVLET_PATH = "/console2";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(NAME,
			"Web administrative console user interface\"", VaadinAuthentication.NAME,
			Collections.singletonMap(SERVLET_PATH, "Web console endpoint"));

	private final ApplicationContext applicationContext;
	private final NetworkServer server;
	private final MessageSource msg;
	private final AdvertisedAddressProvider advertisedAddrProvider;
	private final ObjectFactory<SandboxAuthnRouter> sandboxAuthnRouterFactory;
	private final RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter;

	@Autowired
	public ConsoleEndpointFactory(ApplicationContext applicationContext,
								  NetworkServer server,
								  AdvertisedAddressProvider advertisedAddrProvider,
								  MessageSource msg,
								  ObjectFactory<SandboxAuthnRouter> sandboxAuthnRouterFactory,
								  RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter)
	{
		this.applicationContext = applicationContext;
		this.server = server;
		this.msg = msg;
		this.advertisedAddrProvider = advertisedAddrProvider;
		this.sandboxAuthnRouterFactory = sandboxAuthnRouterFactory;
		this.remoteAuthnResponseProcessingFilter = remoteAuthnResponseProcessingFilter;
	}
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return TYPE;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new SecureVaadin2XEndpoint(server, advertisedAddrProvider, msg, applicationContext, new ConsoleResourceProvider(),
				SERVLET_PATH, remoteAuthnResponseProcessingFilter, sandboxAuthnRouterFactory.getObject(), ConsoleServlet.class);
	}
}
