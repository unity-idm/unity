/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole;

import io.imunity.webelements.navigation.NavigationInfo;

/**
 * Base for all web console navigation info providers. Contains boilerplate code
 * 
 * @author P.Piernik
 *
 */
public class WebConsoleNavigationInfoProviderBase implements WebConsoleNavigationInfoProvider
{
	private NavigationInfo navigationInfo;

	public WebConsoleNavigationInfoProviderBase(NavigationInfo navigationInfo)
	{
		this.navigationInfo = navigationInfo;
	}

	@Override
	public NavigationInfo getNavigationInfo()
	{
		return navigationInfo;
	}

}
