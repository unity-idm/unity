/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman;

import io.imunity.webelements.navigation.NavigationInfo;

/**
 * Base for all upMan navigation info providers. Contains boilerplate code
 * 
 * @author P.Piernik
 *
 */
public class UpManNavigationInfoProviderBase implements UpManNavigationInfoProvider
{
	private NavigationInfo navigationInfo;

	public UpManNavigationInfoProviderBase(NavigationInfo navigationInfo)
	{
		this.navigationInfo = navigationInfo;
	}

	@Override
	public NavigationInfo getNavigationInfo()
	{
		return navigationInfo;
	}

}
