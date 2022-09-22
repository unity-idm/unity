/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.elements;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

@CssImport("./styles/components/menu-button.css")
public class MenuButton extends Div
{
	private final Icon createdIcon;
	public MenuButton(String label, VaadinIcon icon)
	{
		createdIcon = icon.create();
		add(createdIcon, new Span(label));
		addClassName("menu-button");
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		if(enabled)
			createdIcon.removeClassName("disable-icon-color");
		else
			createdIcon.addClassName("disable-icon-color");
	}
}
