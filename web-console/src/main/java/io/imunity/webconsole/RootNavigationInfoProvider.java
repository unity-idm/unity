/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole;

import org.springframework.stereotype.Component;

import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
public class RootNavigationInfoProvider implements WebConsoleNavigationInfoProvider
{		
	public static final String ID = "Root";
	
	@Override
	public NavigationInfo getNavigationInfo()
	{
		return new NavigationInfo.NavigationInfoBuilder(ID, Type.ViewGroup).build();
	}		
}