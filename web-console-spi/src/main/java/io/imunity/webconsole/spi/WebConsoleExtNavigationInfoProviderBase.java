/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.spi;

import io.imunity.webelements.navigation.NavigationInfo;

/**
 * Base for web console extension navigation info providers. Contains boilerplate code
 * 
 * @author P.Piernik
 *
 */
public class WebConsoleExtNavigationInfoProviderBase implements WebConsoleExtNavigationInfoProvider
{
	private NavigationInfo navigationInfo;

	public WebConsoleExtNavigationInfoProviderBase(NavigationInfo navigationInfo)
	{
		this.navigationInfo = new NavigationInfo.NavigationInfoBuilder(navigationInfo.id, navigationInfo.type)
				.withCaption(navigationInfo.caption)
				.withIcon(navigationInfo.icon)
				.withParent(navigationInfo.parent)
				.withPosition(navigationInfo.position + 1000)
				.withShortCaption(navigationInfo.shortCaption)
				.withObjectFactory(navigationInfo.objectFactory)
				.build();
	}

	@Override
	public NavigationInfo getNavigationInfo()
	{
		return navigationInfo;
	}

}
