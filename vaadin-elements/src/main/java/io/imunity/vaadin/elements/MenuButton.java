/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

public class MenuButton extends Div
{
	public MenuButton(String label, VaadinIcon icon)
	{
		Icon createdIcon = icon.create();
		add(createdIcon, new Span(label));
		addClassName("u-menu-button");
	}
}
