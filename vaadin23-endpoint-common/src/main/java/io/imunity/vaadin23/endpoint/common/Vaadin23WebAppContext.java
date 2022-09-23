/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.endpoint.common;

import org.eclipse.jetty.webapp.WebAppContext;

import java.util.Optional;
import java.util.Properties;

public class Vaadin23WebAppContext extends WebAppContext
{
	public final Properties properties;
	public final Vaadin823EndpointProperties vaadin23Properties;

	public Vaadin23WebAppContext(Properties properties, Vaadin823EndpointProperties vaadinEndpointProperties)
	{
		this.properties = properties;
		this.vaadin23Properties = vaadinEndpointProperties;
	}

	public static Properties getCurrentWebAppContextProperties()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin23WebAppContext) context)
				.map(context -> context.properties)
				.orElse(null);
	}

	public static Vaadin823EndpointProperties getCurrentWebAppVaadinProperties()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin23WebAppContext) context)
				.map(context -> context.vaadin23Properties)
				.orElse(null);
	}
}
