/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Label;

@CssImport("./styles/components/input-label.css")
public class InputLabel extends Label
{
	private boolean required;

	public InputLabel(String label)
	{
		add(label);
		addClassName("input-label");
	}

	public void setRequired(boolean required)
	{
		if (required)
		{
			addClassName("indicator");
			this.required = true;
		}
	}

	public void setErrorMode()
	{
		if(required)
		{
			removeClassName("indicator");
			addClassName("error-indicator");
		}
	}

	public void setNormalMode()
	{
		if(required)
		{
			removeClassName("error-indicator");
			addClassName("indicator");
		}
	}
}
