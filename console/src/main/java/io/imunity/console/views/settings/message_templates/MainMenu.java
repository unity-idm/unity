/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.message_templates;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin.elements.MenuButton;

import java.util.Set;

class MainMenu
{
	Component menu;
	Set<MenuButton> menuButtons;

	public MainMenu(Component menu, Set<MenuButton> menuButtons)
	{
		this.menu = menu;
		this.menuButtons = menuButtons;
	}

	void setEnabled(boolean value)
	{
		menuButtons.forEach(menuButton -> menuButton.setEnabled(value));
	}
}
