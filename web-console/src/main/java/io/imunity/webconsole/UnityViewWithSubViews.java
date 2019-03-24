/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole;

import io.imunity.webelements.navigation.UnityView;

/**
 * 
 * @author P.Piernik
 *
 */
public interface UnityViewWithSubViews extends UnityView
{
	BreadcrumbsComponent getBreadcrumbsComponent();
}
