/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.spi;

import org.springframework.stereotype.Component;

import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;

/**
 * Root for all navigable web console extensions {@link UnityView}s
 * 
 * @author P.Piernik
 *
 */
@Component
public class WebConsoleExtRootNavigationInfoProvider extends WebConsoleExtNavigationInfoProviderBase
{
	public static final String ID = "ExtRoot";

	public WebConsoleExtRootNavigationInfoProvider()
	{
		super(new NavigationInfo.NavigationInfoBuilder(ID, Type.ViewGroup).build());

	}
}