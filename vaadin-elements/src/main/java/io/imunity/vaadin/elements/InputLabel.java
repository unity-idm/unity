/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.html.Span;

public class InputLabel extends Span
{
	private boolean required;

	public InputLabel(String label)
	{
		setText(label);
		addClassName("input-label");
	}

	@Override
	public void setText(String text)
	{
		super.setText(text);
		if(text == null || text.isBlank())
			getStyle().set("padding-top" ,"0");
		else
			getStyle().set("padding-top" ,"var(--input-label-top-padding)");
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
