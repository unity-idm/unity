/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.helpers;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.UI;

/**
 * Helper for navigating through views
 * 
 * @author P.Piernik
 *
 */
public class NavigationHelper
{
	public static enum CommonViewParam { name };
	
	
	public static String getParam(ViewChangeEvent event, String paramName)
	{
		return event.getParameterMap().isEmpty()
				|| !event.getParameterMap().containsKey(paramName) ? ""
						: event.getParameterMap().get(paramName);

	}

	public static void goToView(String viewName)
	{
		UI.getCurrent().getNavigator().navigateTo(viewName);
	}
}
