/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Factory creating endpoints exposing {@link UserHomeUI}.
 * 
 * @author K. Benedyczak
 */
@Component
public class UserHomeEndpointFactory implements EndpointFactory
{
	public static final String NAME = "UserHomeUI";
	public static final String SERVLET_PATH = "/home";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(NAME,
			"User-oriented account management web interface", VaadinAuthentication.NAME,
			Collections.singletonMap(SERVLET_PATH, "User home endpoint"));

	private ApplicationContext applicationContext;
	private NetworkServer server;
	private UnityMessageSource msg;
	private AdvertisedAddressProvider advertisedAddrProvider;

	@Autowired
	public UserHomeEndpointFactory(ApplicationContext applicationContext,
			NetworkServer server,
			AdvertisedAddressProvider advertisedAddrProvider,
			UnityMessageSource msg)
	{
		this.applicationContext = applicationContext;
		this.server = server;
		this.msg = msg;
		this.advertisedAddrProvider = advertisedAddrProvider;
	}

	@Override
	public EndpointTypeDescription getDescription()
	{
		return TYPE;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new VaadinEndpoint(server, advertisedAddrProvider, msg, applicationContext, UserHomeUI.class.getSimpleName(),
				SERVLET_PATH);
	}
}
