/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman;

import org.springframework.stereotype.Component;

import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;

/**
 * Root for all navigable upMan {@link UnityView}
 * 
 * @author P.Piernik
 *
 */
@Component
public class UpManRootNavigationInfoProvider extends UpManNavigationInfoProviderBase
{
	public static final String ID = "Root";

	public UpManRootNavigationInfoProvider()
	{
		super(new NavigationInfo.NavigationInfoBuilder(ID, Type.ViewGroup).build());

	}
}