/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.console.views.identity_provider.released_profile.endpoints.spi;

import com.vaadin.flow.component.icon.VaadinIcon;

public class ServiceActionRepresentation
{
	public final String caption;
	public final VaadinIcon icon;

	public ServiceActionRepresentation(String caption, VaadinIcon icon)
	{
		this.caption = caption;
		this.icon = icon;
	}
}