/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.shared.ui.Transport;
import io.imunity.vaadin.endpoint.common.FaviconSetuper;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

@Push(transport = Transport.LONG_POLLING)
public class UpmanShellConfigurator implements AppShellConfigurator
{
	private final UnityServerConfiguration config;

	@Autowired
	UpmanShellConfigurator(UnityServerConfiguration config)
	{
		this.config = config;
	}
	@Override
	public void configurePage(AppShellSettings settings)
	{
		FaviconSetuper.setupFavicon(settings, config);
	}

}
