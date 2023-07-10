/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views;

import io.imunity.vaadin.elements.UnityViewComponent;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppDisplayedName;

public class ConsoleViewComponent extends UnityViewComponent
{

	public final String pageTitle = getCurrentWebAppDisplayedName();

	@Override
	public String getPageTitle()
	{
		return pageTitle;
	}
}
