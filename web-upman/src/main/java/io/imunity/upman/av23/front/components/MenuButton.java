/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.av23.front.components;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

@CssImport("./styles/components/menu-button.css")
public class MenuButton extends Div {

	public MenuButton(String label, VaadinIcon icon) {
		super(icon.create(), new Span(label));
		addClassName("menu-button");
	}
	
	public MenuButton(VaadinIcon icon) {
		this(null, icon);
	}
}
