/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.navigation;

/**
 * Provides {@link NavigationInfo} - static informations about @{link
 * {@link UnityView}, which are used to build a menu and breadcrumbs.
 * 
 * @author P.Piernik
 *
 */
public interface NavigationInfoProvider
{

	NavigationInfo getNavigationInfo();

}
