/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.menu;

import com.vaadin.ui.Component;

/**
 * Single menu element
 * @author P.Piernik
 *
 */
public interface MenuElement extends Component
{
	void setActive(boolean active);
	String getMenuElementId();
}
