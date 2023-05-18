/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;
import io.imunity.vaadin.endpoint.common.FaviconSetuper;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

@Theme("home-theme")
class UserHomeShellConfigurator implements AppShellConfigurator
{
	private final UnityServerConfiguration config;

	@Autowired
	UserHomeShellConfigurator(UnityServerConfiguration config)
	{
		this.config = config;
	}

	@Override
	public void configurePage(AppShellSettings settings)
	{
		FaviconSetuper.setupFavicon(settings, config);
	}
}
