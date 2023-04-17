/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import org.eclipse.jetty.webapp.WebAppContext;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.authn.CancelHandler;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class Vaadin2XWebAppContext extends WebAppContext
{
	public final Properties properties;
	public final Vaadin82XEndpointProperties vaadin23Properties;
	public final MessageSource messageSource;
	public final ResolvedEndpoint description;
	public final CancelHandler cancelHandler;
	public List<AuthenticationFlow> authenticationFlows;

	public Vaadin2XWebAppContext(Properties properties, Vaadin82XEndpointProperties vaadinEndpointProperties,
	                             MessageSource messageSource, ResolvedEndpoint description)
	{
		this.properties = properties;
		this.vaadin23Properties = vaadinEndpointProperties;
		this.messageSource = messageSource;
		this.description = description;
		this.authenticationFlows = List.of();
		this.cancelHandler = null;
	}

	private synchronized void setAuthenticationFlows(List<AuthenticationFlow> authenticationFlows)
	{
		this.authenticationFlows = authenticationFlows;
	}

	public Vaadin2XWebAppContext(Properties properties, Vaadin82XEndpointProperties vaadinEndpointProperties,
	                             MessageSource messageSource, ResolvedEndpoint description, List<AuthenticationFlow> authenticationFlows,
	                             CancelHandler cancelHandler)
	{
		this.properties = properties;
		this.vaadin23Properties = vaadinEndpointProperties;
		this.messageSource = messageSource;
		this.description = description;
		this.authenticationFlows = authenticationFlows;
		this.cancelHandler = cancelHandler;
	}

	public static Properties getCurrentWebAppContextProperties()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin2XWebAppContext) context)
				.map(context -> context.properties)
				.orElse(null);
	}

	public static Vaadin82XEndpointProperties getCurrentWebAppVaadinProperties()
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
				.orElse(null);
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

	static void setCurrentWebAppAuthenticationFlows(List<AuthenticationFlow> authenticationFlows)
	{
		List<AuthenticationFlow> flows = List.copyOf(authenticationFlows);
		Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin2XWebAppContext) context)
				.ifPresent(context -> context.setAuthenticationFlows(flows));
	}
}
