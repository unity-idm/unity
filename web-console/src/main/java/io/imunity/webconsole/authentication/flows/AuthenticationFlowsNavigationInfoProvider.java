/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.flows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.facilities.AuthenticationFacilitiesView.AuthenticationFacilitiesNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Provides @{link {@link NavigationInfo} about authentication flows
 * 
 * @author P.Piernik
 *
 */
@Component
class AuthenticationFlowsNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
{
	public static final String ID = "AuthenticationFlows";

	@Autowired
	AuthenticationFlowsNavigationInfoProvider(MessageSource msg,
			AuthenticationFacilitiesNavigationInfoProvider parent)
	{
		super(new NavigationInfo.NavigationInfoBuilder(ID, Type.ViewGroup)
				.withParent(AuthenticationFacilitiesNavigationInfoProvider.ID)
				.withCaption(msg.getMessage("AuthenticationFlows.navCaption"))
				.build());

	}

}
