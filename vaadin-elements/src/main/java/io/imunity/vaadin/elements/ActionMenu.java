/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.Icon;

import static com.vaadin.flow.component.icon.VaadinIcon.MENU;

@CssImport("./styles/components/action-menu.css")
public class ActionMenu extends ContextMenu
{
	public ActionMenu()
	{
		super(MENU.create());
		Icon target = (Icon)getTarget();
		target.getStyle().set("cursor", "pointer");
		setOpenOnClick(true);
		addClassName("action-menu");
	}

}
