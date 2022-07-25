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

	public Vaadin23WebAppContext(Properties properties)
	{
		this.properties = properties;
	}

	public static Properties getCurrentWebAppContextProperties()
	{
		return Optional.ofNullable(getCurrentWebAppContext())
				.map(context -> (Vaadin23WebAppContext) context)
				.map(context -> context.properties)
				.orElse(null);
	}
}
