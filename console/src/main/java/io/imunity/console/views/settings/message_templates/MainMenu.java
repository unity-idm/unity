/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.message_templates;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin.elements.MenuButton;

class MainMenu
{
	public final Component menu;
	public final MenuButton removeButton;
	public final MenuButton resetButton;

	MainMenu(Component menu, MenuButton removeButton, MenuButton resetButton)
	{
		this.menu = menu;
		this.removeButton = removeButton;
		this.resetButton = resetButton;
	}

	void setEnabled(boolean value)
	{
		removeButton.setEnabled(value);
		resetButton.setEnabled(value);
	}

	void setResetButtonEnabled(boolean value)
	{
		resetButton.setEnabled(value);
	}
}
