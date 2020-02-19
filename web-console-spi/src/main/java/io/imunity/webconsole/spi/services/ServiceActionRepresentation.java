/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.spi.services;

import com.vaadin.server.Resource;

public class ServiceActionRepresentation
{
	public final String caption;
	public final Resource icon;

	public ServiceActionRepresentation(String caption, Resource icon)
	{
		this.caption = caption;
		this.icon = icon;
	}
}