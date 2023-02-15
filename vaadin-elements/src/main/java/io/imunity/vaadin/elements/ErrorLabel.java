/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Label;

@CssImport("./styles/components/error-label.css")
public class ErrorLabel extends Label
{
	public ErrorLabel(String label)
	{
		add(label);
		addClassName("error-label");
	}
}
