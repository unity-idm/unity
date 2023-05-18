/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.home.views;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.HasDynamicTitle;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppDisplayedName;

public abstract class HomeViewComponent extends Composite<Div> implements AfterNavigationObserver, HasDynamicTitle
{
	public final String pageTitle = getCurrentWebAppDisplayedName();

	@Override
	public String getPageTitle() {
		return pageTitle;
	}
}
