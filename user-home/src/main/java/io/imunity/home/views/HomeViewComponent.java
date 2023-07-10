/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.home.views;

import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import io.imunity.vaadin.elements.UnityViewComponent;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppDisplayedName;

public abstract class HomeViewComponent extends UnityViewComponent implements AfterNavigationObserver, HasDynamicTitle
{
	public final String pageTitle = getCurrentWebAppDisplayedName();

	@Override
	public String getPageTitle() {
		return pageTitle;
	}
}
