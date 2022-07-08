/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.av23.front.components;

import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.CssImport;

import static com.vaadin.flow.component.icon.VaadinIcon.MENU;

@CssImport("./styles/components/grid-action-menu.css")
public class GridActionMenu extends ContextMenu {

	public GridActionMenu() {
		super(new MenuButton(MENU));
		setOpenOnClick(true);
		addClassName("grid-action-menu");
	}

}
