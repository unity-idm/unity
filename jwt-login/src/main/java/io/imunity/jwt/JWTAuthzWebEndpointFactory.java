/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt;

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
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;

@Component
public class JWTAuthzWebEndpointFactory implements EndpointFactory
{
	public static final String NAME = "JWTAuthz";
	public static final String SERVLET_PATH = "/jwt-authz";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "JWT authorization UI that allows web SSO session creation", 
			VaadinAuthentication.NAME,
			Collections.singletonMap(SERVLET_PATH, "JWT Authorization web endpoint"));
	
	private ApplicationContext applicationContext;
	private NetworkServer server;
	private MessageSource msg;
	private AdvertisedAddressProvider advertisedAddrProvider;
	private RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter;

	@Autowired
	public JWTAuthzWebEndpointFactory(ApplicationContext applicationContext,
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
		return new VaadinEndpoint(server, advertisedAddrProvider, msg, applicationContext,
				JWTAuthzUI.class.getSimpleName(), SERVLET_PATH, remoteAuthnResponseProcessingFilter);
	}
}
