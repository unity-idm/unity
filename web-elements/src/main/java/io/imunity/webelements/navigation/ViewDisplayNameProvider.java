/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webelements.navigation;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;

public interface ViewDisplayNameProvider
{
	String getDisplayName(ViewChangeEvent event);
}
