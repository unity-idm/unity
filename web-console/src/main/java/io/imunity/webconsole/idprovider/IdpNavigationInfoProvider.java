/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.idprovider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.WebConsoleRootNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Provides @{link {@link NavigationInfo} about idp providers submenu
 * 
 * @author P.Piernik
 *
 */
@Component
public class IdpNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
{
	public static final String ID = "IdpProvider";

	@Autowired
	public IdpNavigationInfoProvider(UnityMessageSource msg,
			WebConsoleRootNavigationInfoProvider parent)
	{
		super(new NavigationInfo.NavigationInfoBuilder(ID, Type.ViewGroup)
				.withParent(parent.getNavigationInfo())
				.withCaption(msg.getMessage("WebConsoleMenu.idpProvider"))
				.withIcon(Images.globe.getResource()).withPosition(2).build());

	}
}
