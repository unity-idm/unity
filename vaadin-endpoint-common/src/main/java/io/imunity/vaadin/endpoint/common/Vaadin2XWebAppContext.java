/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import org.eclipse.jetty.ee10.webapp.WebAppContext;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class Vaadin2XWebAppContext extends WebAppContext
{
	public final Properties properties;
	public final VaadinEndpointProperties vaadin23Properties;
	public final MessageSource messageSource;
	public final ResolvedEndpoint description;
	public final CancelHandler cancelHandler;
	public final SandboxAuthnRouter sandboxRouter;

	public List<AuthenticationFlow> authenticationFlows;

	public Vaadin2XWebAppContext(Properties properties, VaadinEndpointProperties vaadinEndpointProperties,
	                             MessageSource messageSource, ResolvedEndpoint description)
	{
		this(properties, vaadinEndpointProperties, messageSource, description, List.of(), null);
	}

	void setAuthenticationFlows(List<AuthenticationFlow> authenticationFlows)
	{
		this.authenticationFlows = authenticationFlows;
	}

	public Vaadin2XWebAppContext(Properties properties, VaadinEndpointProperties vaadinEndpointProperties,
	                             MessageSource messageSource, ResolvedEndpoint description, List<AuthenticationFlow> authenticationFlows,
	                             CancelHandler cancelHandler)
	{
		this(properties, vaadinEndpointProperties, messageSource, description, authenticationFlows, cancelHandler, null);
	}

	public Vaadin2XWebAppContext(Properties properties, VaadinEndpointProperties vaadinEndpointProperties,
	                             MessageSource messageSource, ResolvedEndpoint description, List<AuthenticationFlow> authenticationFlows,
	                             CancelHandler cancelHandler, SandboxAuthnRouter sandboxRouter)
	{
		this.properties = properties;
		this.vaadin23Properties = vaadinEndpointProperties;
		this.messageSource = messageSource;
		this.description = description;
		this.authenticationFlows = authenticationFlows;
		this.cancelHandler = cancelHandler;
		this.sandboxRouter = sandboxRouter;
	}

	public static Properties getCurrentWebAppContextProperties()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin2XWebAppContext) context)
				.map(context -> context.properties)
				.orElse(null);
	}

	public static VaadinEndpointProperties getCurrentWebAppVaadinProperties()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin2XWebAppContext) context)
				.map(context -> context.vaadin23Properties)
				.orElse(null);
	}

	public static String getCurrentWebAppDisplayedName()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin2XWebAppContext) context)
				.map(context -> context.description.getEndpoint()
						.getConfiguration()
						.getDisplayedName()
						.getValue(context.messageSource)
				)
				.orElse("");
	}

	public static Endpoint getCurrentWebAppEndpoint()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin2XWebAppContext) context)
				.map(context -> context.description.getEndpoint())
				.orElse(null);
	}

	public static ResolvedEndpoint getCurrentWebAppResolvedEndpoint()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin2XWebAppContext) context)
				.map(context -> context.description)
				.orElse(null);
	}

	public static List<AuthenticationFlow> getCurrentWebAppAuthenticationFlows()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin2XWebAppContext) context)
				.map(context -> context.authenticationFlows)
				.orElse(List.of());
	}

	public static CancelHandler getCurrentWebAppCancelHandler()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin2XWebAppContext) context)
				.map(context -> context.cancelHandler)
				.orElse(null);
	}

	public static SandboxAuthnRouter getCurrentWebAppSandboxAuthnRouter()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin2XWebAppContext) context)
				.map(context -> context.sandboxRouter)
				.orElse(null);
	}
}
