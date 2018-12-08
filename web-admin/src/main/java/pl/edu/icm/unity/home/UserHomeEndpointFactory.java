/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Factory creating endpoints exposing {@link UserHomeUI}.
 * @author K. Benedyczak
 */
@Component
public class UserHomeEndpointFactory implements EndpointFactory
{
	public static final String NAME = "UserHomeUI";
	public static final String SERVLET_PATH = "/home";

	private EndpointTypeDescription description;
	private ApplicationContext applicationContext;
	private NetworkServer server;
	private UnityMessageSource msg;
	
	@Autowired
	public UserHomeEndpointFactory(ApplicationContext applicationContext, NetworkServer server,
			UnityMessageSource msg)
	{
		this.applicationContext = applicationContext;
		this.server = server;
		this.msg = msg;
		
		Map<String,String> paths=new HashMap<>();
		paths.put(SERVLET_PATH,"User home endpoint");
		description = new EndpointTypeDescription(NAME, 
				"User-oriented account management web interface", VaadinAuthentication.NAME, paths);
	}
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return description;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new VaadinEndpoint(server, msg, applicationContext, 
				UserHomeUI.class.getSimpleName(), SERVLET_PATH);
	}
}
