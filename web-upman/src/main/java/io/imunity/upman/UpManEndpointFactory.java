/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.upman;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.ProjectManagementConstants;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Factory creating endpoints exposing {@link UpManUI}.
 * @author P.Piernik
 *
 */
@Component
public class UpManEndpointFactory implements EndpointFactory
{
	public static final String NAME = ProjectManagementConstants.ENDPOINT_NAME;
	public static final String SERVLET_PATH = "/upman";

	private EndpointTypeDescription description;
	private ApplicationContext applicationContext;
	private NetworkServer server;
	private UnityMessageSource msg;
	
	@Autowired
	public UpManEndpointFactory(ApplicationContext applicationContext, NetworkServer server,
			UnityMessageSource msg)
	{
		this.applicationContext = applicationContext;
		this.server = server;
		this.msg = msg;
		
		Map<String,String> paths = new HashMap<>();
		paths.put(SERVLET_PATH, "Group management endpoint");
		description = new EndpointTypeDescription(NAME, 
				"Web group management user interface", VaadinAuthentication.NAME, paths);
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
			UpManUI.class.getSimpleName(), SERVLET_PATH);
	}
}
