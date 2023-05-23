/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;

public class LinkButton extends Div
{
	private Label label;

	public LinkButton(String txt, ComponentEventListener<ClickEvent<Div>> listener)
	{
		label = new Label(txt);
		label.getStyle().set("cursor", "pointer");
		add(label);
		getStyle().set("text-decoration", "underline");
		getStyle().set("cursor", "pointer");
		addClickListener(listener);
	}

	public void setLabel(String label)
	{
		this.label.setText(label);
	}
}
