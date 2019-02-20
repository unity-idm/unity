/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.WebConsoleRootNavigationInfoProvider;
import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Provides @{link {@link NavigationInfo} about settings submenu
 * 
 * @author P.Piernik
 *
 */
@Component
public class SettingsNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
{
	public static final String ID = "Settings";

	@Autowired
	public SettingsNavigationInfoProvider(UnityMessageSource msg,
			WebConsoleRootNavigationInfoProvider parent)
	{
		super(new NavigationInfo.NavigationInfoBuilder(ID, Type.ViewGroup)
				.withParent(parent.getNavigationInfo())
				.withCaption(msg.getMessage("WebConsoleMenu.settings"))
				.withIcon(Images.settings.getResource()).withPosition(5).build());

	}

}
