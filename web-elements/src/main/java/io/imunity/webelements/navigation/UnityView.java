/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.navigation;

import com.vaadin.navigator.View;

/**
 * In principle all View should implement this interface. Displayed name is used
 * to show name of element displayed by view which implements this interface
 * 
 * @author P.Piernik
 *
 */
public interface UnityView extends View
{
	String getViewName();
	String getDisplayedName();
}
