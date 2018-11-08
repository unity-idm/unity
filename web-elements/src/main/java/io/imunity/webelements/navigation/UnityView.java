/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.navigation;

import com.vaadin.navigator.View;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

import io.imunity.webelements.layout.BreadCrumbs;

/**
 * In principle all View should implement this interface. Displayed name is used
 * by {@link BreadCrumbs} component to show name of element displayed by view
 * which implements this interface
 * 
 * @author P.Piernik
 *
 */
public interface UnityView extends View
{
	String getDisplayedName();
	default Component getViewHeader()
	{
		return new Label(getDisplayedName());
	}
}
