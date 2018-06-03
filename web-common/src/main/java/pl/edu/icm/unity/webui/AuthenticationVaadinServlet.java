/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.List;
import java.util.Properties;

import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;


/**
 * The same as {@link UnityVaadinServlet}, but hardcodes use of {@link AuthenticationUI} and ensures that
 * proper theme is used.
 * @author K. Benedyczak
 */
public class AuthenticationVaadinServlet extends UnityVaadinServlet
{
	public AuthenticationVaadinServlet(ApplicationContext applicationContext, 
			ResolvedEndpoint description,
			List<AuthenticationFlow> authenticationFlows,
			EndpointRegistrationConfiguration registrationConfiguration,
			Properties endpointProperties,
			UnityBootstrapHandler bootstrapHandler)
	{
		super(applicationContext, AuthenticationUI.class.getSimpleName(), description, authenticationFlows, 
				registrationConfiguration, 
				endpointProperties, bootstrapHandler, VaadinEndpointProperties.AUTHN_THEME);
	}
}
