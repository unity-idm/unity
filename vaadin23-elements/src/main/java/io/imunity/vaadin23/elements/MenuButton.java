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
	private final String defaultColor;
	public MenuButton(String label, VaadinIcon icon)
	{
		createdIcon = icon.create();
		defaultColor = createdIcon.getColor();
		add(createdIcon, new Span(label));
		addClassName("menu-button");
	}
	
	public MenuButton(VaadinIcon icon)
	{
		this(null, icon);
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		if(enabled)
			createdIcon.setColor(defaultColor);
		else
			createdIcon.setColor("#cfd4db");
	}
}
