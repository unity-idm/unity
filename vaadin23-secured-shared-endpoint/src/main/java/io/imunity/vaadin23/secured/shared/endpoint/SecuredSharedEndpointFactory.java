/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.vaadin23.secured.shared.endpoint;

import io.imunity.vaadin23.endpoint.common.Vaadin823Endpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;

import java.util.Collections;


@Component
public class SecuredSharedEndpointFactory implements EndpointFactory
{
	public static final String NAME = "WellKnownLinksHandler2";
	public static final String SERVLET_PATH = "/well-known";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(NAME,
			"Provides access to public links which can be used to access parts of Unity UIs directly", VaadinAuthentication.NAME,
			Collections.singletonMap(SERVLET_PATH, "Group management endpoint"));

	private final ApplicationContext applicationContext;
	private final NetworkServer server;
	private final MessageSource msg;
	private final AdvertisedAddressProvider advertisedAddrProvider;
	private final RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter;

	@Autowired
	public SecuredSharedEndpointFactory(ApplicationContext applicationContext,
	                                    NetworkServer server,
	                                    AdvertisedAddressProvider advertisedAddrProvider,
	                                    MessageSource msg,
	                                    RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter)
	{
		this.applicationContext = applicationContext;
		this.server = server;
		this.msg = msg;
		this.advertisedAddrProvider = advertisedAddrProvider;
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
		return new Vaadin823Endpoint(server, advertisedAddrProvider, msg, applicationContext,
				new SecuredSharedResourceProvider(), SERVLET_PATH, remoteAuthnResponseProcessingFilter, SharedVaadin23Servlet.class);
	}
}
