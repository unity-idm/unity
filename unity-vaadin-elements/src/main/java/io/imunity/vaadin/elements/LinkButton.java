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
	public LinkButton(String txt, ComponentEventListener<ClickEvent<Div>> listener)
	{
		Label cancelLabel = new Label(txt);
		cancelLabel.getStyle().set("cursor", "pointer");
		add(cancelLabel);
		getStyle().set("text-decoration", "underline");
		getStyle().set("cursor", "pointer");
		addClickListener(listener);
	}
}
