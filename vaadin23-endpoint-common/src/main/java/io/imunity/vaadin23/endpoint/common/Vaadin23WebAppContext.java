/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.endpoint.common;

import org.eclipse.jetty.webapp.WebAppContext;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;

import java.util.Optional;
import java.util.Properties;

public class Vaadin23WebAppContext extends WebAppContext
{
	public final Properties properties;
	public final VaadinEndpointProperties vaadinEndpointProperties;

	public Vaadin23WebAppContext(Properties properties, VaadinEndpointProperties vaadinEndpointProperties)
	{
		this.properties = properties;
		this.vaadinEndpointProperties = vaadinEndpointProperties;
	}

	public static Properties getCurrentWebAppContextProperties()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin23WebAppContext) context)
				.map(context -> context.properties)
				.orElse(null);
	}

	public static VaadinEndpointProperties getCurrentWebAppVaadinProperties()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin23WebAppContext) context)
				.map(context -> context.vaadinEndpointProperties)
				.orElse(null);
	}
}
