/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.elements;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.CssImport;

import static com.vaadin.flow.component.icon.VaadinIcon.MENU;

@CssImport("./styles/components/action-menu.css")
public class ActionMenu extends ContextMenu
{
	public ActionMenu()
	{
		super(new MenuButton(MENU));
		setOpenOnClick(true);
		addClassName("action-menu");
	}

}
