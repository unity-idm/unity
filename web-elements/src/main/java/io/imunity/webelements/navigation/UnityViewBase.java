/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.navigation;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.UI;

/**
 * Abstract base for all unity view
 * 
 * @author P.Piernik
 *
 */
public abstract class UnityViewBase extends CustomComponent implements UnityView
{

	protected String getParam(ViewChangeEvent event, String paramName)
	{
		return event.getParameterMap().isEmpty()
				|| !event.getParameterMap().containsKey(paramName) ? ""
						: event.getParameterMap().get(paramName);

	}

	protected void goToView(String viewName)
	{
		UI.getCurrent().getNavigator().navigateTo(viewName);
	}
}
