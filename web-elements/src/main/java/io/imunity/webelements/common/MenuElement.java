/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.common;

import com.vaadin.ui.Component;

/**
 * 
 * @author P.Piernik
 *
 */
public interface MenuElement extends Component
{
	void activate();
	void deactivate();
	String getMenuElementId();
}
