/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.InsecureVaadinEndpoint;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;

/**
 * Factory creating endpoints exposing {@link AttrIntrospectionUI}.
 * 
 * @author P.Piernik
 *
 */
@Component
public class AttrIntrospectionEndpointFactory implements EndpointFactory
{
	public static final String NAME = "AttributeIntrospection";
	public static final String SERVLET_PATH = "/introspection";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(NAME,
			"Attribute introspection endpoint", VaadinAuthentication.NAME,
			Collections.singletonMap(SERVLET_PATH, "Attribute introspection endpoint"));

	private ApplicationContext applicationContext;
	private NetworkServer server;
	private MessageSource msg;
	private AdvertisedAddressProvider advertisedAddrProvider;
	private RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter;

	@Autowired
	AttrIntrospectionEndpointFactory(ApplicationContext applicationContext, NetworkServer server,
			AdvertisedAddressProvider advertisedAddrProvider, MessageSource msg,
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
		return new InsecureVaadinEndpoint(server, advertisedAddrProvider, msg, applicationContext,
				AttrIntrospectionUI.class.getSimpleName(), SERVLET_PATH, remoteAuthnResponseProcessingFilter);
	}
}
