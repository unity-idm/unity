/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
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

@Component
public class IdentityProviderNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
{
	public static final String ID = "IdentityProvider";

	@Autowired
	public IdentityProviderNavigationInfoProvider(UnityMessageSource msg,
			WebConsoleRootNavigationInfoProvider parent)
	{
		super(new NavigationInfo.NavigationInfoBuilder(ID, Type.ViewGroup)
				.withParent(parent.getNavigationInfo())
				.withCaption(msg.getMessage("WebConsoleMenu.identityProvider"))
				.withIcon(Images.globe_wire.getResource()).withPosition(40).build());

	}
}
