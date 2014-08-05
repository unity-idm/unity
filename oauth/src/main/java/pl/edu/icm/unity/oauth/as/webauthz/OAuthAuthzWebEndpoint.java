/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.VaadinEndpoint;

/**
 * OAuth2 authorization endpoint, Vaadin based.
 * @author K. Benedyczak
 */
public class OAuthAuthzWebEndpoint extends VaadinEndpoint
{
	public OAuthAuthzWebEndpoint(EndpointTypeDescription type,
			ApplicationContext applicationContext, String servletPath)
	{
		super(type, applicationContext, OAuthAuthzUI.class.getSimpleName(), servletPath);
	}

	
}
