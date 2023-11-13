/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import static io.imunity.vaadin.elements.VaadinClassNames.POINTER;

public class LinkButton extends Div
{
	private final Span label;

	public LinkButton(String txt, ComponentEventListener<ClickEvent<Div>> listener)
	{
		label = new Span(txt);
		label.addClassName(POINTER.getName());
		add(label);
		getStyle().set("text-decoration", "underline");
		addClassName(POINTER.getName());
		addClickListener(listener);
	}

	public void setLabel(String label)
	{
		this.label.setText(label);
	}
}
