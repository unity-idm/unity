/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.icon.Icon;

import static com.vaadin.flow.component.icon.VaadinIcon.MENU;
import static io.imunity.vaadin.elements.VaadinClassNames.POINTER;

public class ActionMenu extends ContextMenu
{
	public ActionMenu()
	{
		super(MENU.create());
		Icon target = (Icon)getTarget();
		target.addClassName(POINTER.getName());
		setOpenOnClick(true);
		addClassName("u-action-menu");
	}

}
