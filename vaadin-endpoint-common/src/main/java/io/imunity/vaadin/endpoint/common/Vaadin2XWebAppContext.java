/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import org.eclipse.jetty.webapp.WebAppContext;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

import java.util.Optional;
import java.util.Properties;

public class Vaadin2XWebAppContext extends WebAppContext
{
	public final Properties properties;
	public final Vaadin82XEndpointProperties vaadin23Properties;
	public final MessageSource messageSource;
	public final ResolvedEndpoint description;

	public Vaadin2XWebAppContext(Properties properties, Vaadin82XEndpointProperties vaadinEndpointProperties,
	                             MessageSource messageSource, ResolvedEndpoint description)
	{
		this.properties = properties;
		this.vaadin23Properties = vaadinEndpointProperties;
		this.messageSource = messageSource;
		this.description = description;
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
}
