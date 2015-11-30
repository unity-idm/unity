/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.wellknownurl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

@Component
public class WellKnownURLEndpointFactory implements EndpointFactory
{
	public static final String NAME = "WellKnownLinksHandler";
	public static final String SERVLET_PATH = "/sec";

	private EndpointTypeDescription description;
	private ApplicationContext applicationContext;
	private NetworkServer server;
	private UnityMessageSource msg;
	
	@Autowired
	public WellKnownURLEndpointFactory(ApplicationContext applicationContext, NetworkServer server, 
			UnityMessageSource msg)
	{
		this.applicationContext = applicationContext;
		this.server = server;
		this.msg = msg;
		
		Set<String> supportedAuthn = new HashSet<String>();
		supportedAuthn.add(VaadinAuthentication.NAME);
		Map<String,String> paths=new HashMap<String, String>();
		paths.put(SERVLET_PATH, "Well known links endpoint");
		description = new EndpointTypeDescription(NAME, 
				"Provides access to public links which can be used to access parts of "
				+ "Unity UIs directly", supportedAuthn, paths);
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
				SecuredNavigationUI.class.getSimpleName(), SERVLET_PATH);
	}

}
