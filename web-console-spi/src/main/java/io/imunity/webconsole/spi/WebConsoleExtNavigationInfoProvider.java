/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.spi;

import io.imunity.webelements.navigation.NavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationHierarchyManager;

/**
 * Dummy interface acting as an indicator for {@link NavigationHierarchyManager} about
 * which extensions views should be loaded
 * 
 * @author P.Piernik
 *
 */
public interface WebConsoleExtNavigationInfoProvider extends NavigationInfoProvider
{

}
