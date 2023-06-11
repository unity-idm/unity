/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.WebConsoleRootNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Provides @{link {@link NavigationInfo} about directory setup submenu
 * 
 * @author P.Piernik
 *
 */
@Component
public class DirectorySetupNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
{
	public static final String ID = "DirectorySetup";

	@Autowired
	public DirectorySetupNavigationInfoProvider(MessageSource msg)
	{
		super(new NavigationInfo.NavigationInfoBuilder(ID, Type.ViewGroup)
				.withParent(WebConsoleRootNavigationInfoProvider.ID)
				.withCaption(msg.getMessage("WebConsoleMenu.directorySetup"))
				.withIcon(Images.file_tree.getResource()).withPosition(70).build());

	}

}
