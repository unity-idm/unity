/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.components;

import com.vaadin.flow.component.html.Label;

public class FormLayoutLabel extends Label
{
	public FormLayoutLabel(String text)
	{
		super(text);
		getStyle().set("color", "var(--lumo-body-text-color)");
		getStyle().set("font-size", "var(--lumo-font-size-m)");
	}
}
