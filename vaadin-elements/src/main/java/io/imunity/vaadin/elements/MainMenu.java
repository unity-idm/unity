/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.Component;

import java.util.Set;

public class MainMenu
{
	public final Component menu;
	public final Set<MenuButton> menuButtons;

	public MainMenu(Component menu, Set<MenuButton> menuButtons)
	{
		this.menu = menu;
		this.menuButtons = Set.copyOf(menuButtons);
	}

	public void setEnabled(boolean value)
	{
		menuButtons.forEach(menuButton -> menuButton.setEnabled(value));
	}
}
