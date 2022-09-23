/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@Theme(themeClass = Lumo.class)
public class UnityShellConfigurator implements AppShellConfigurator
{
	@Override
	public void configurePage(AppShellSettings settings)
	{
		settings.addLink("shortcut icon", "/unitygw/VAADIN/favicon.ico");
	}
}
