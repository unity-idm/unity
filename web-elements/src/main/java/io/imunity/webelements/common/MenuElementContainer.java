/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.common;

import java.util.Collection;

/**
 * 
 * @author P.Piernik
 *
 */
public interface MenuElementContainer extends MenuElement
{
	void addMenuElement(MenuElement entry);
	Collection<MenuElement> getMenuElements();

}
