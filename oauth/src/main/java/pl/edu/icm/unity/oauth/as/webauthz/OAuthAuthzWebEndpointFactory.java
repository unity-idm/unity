/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Factory of OAuth 2, Vaadin based authorization endpoints.
 * @author K. Benedyczak
 */
@Component
public class OAuthAuthzWebEndpointFactory implements EndpointFactory
{
	public static final String NAME = "OAuth2Authz";
	public static final String OAUTH_UI_SERVLET_PATH = "/oauth2-authz";
	private EndpointTypeDescription description;
	
	private ApplicationContext applicationContext;
	
	@Autowired
	public OAuthAuthzWebEndpointFactory(ApplicationContext applicationContext)
	{
		this.applicationContext = applicationContext;
		
		Set<String> supportedAuthn = new HashSet<String>();
		supportedAuthn.add(VaadinAuthentication.NAME);
		Map<String, String> paths = new HashMap<String, String>();
		paths.put(OAUTH_UI_SERVLET_PATH, "OAuth 2 Authorization Grant web endpoint");
		description = new EndpointTypeDescription(NAME, 
				"OAuth 2 Server - Authorization Grant endpoint", supportedAuthn, paths);
	}
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return description;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new OAuthAuthzWebEndpoint(description, applicationContext, OAUTH_UI_SERVLET_PATH);
	}
}
