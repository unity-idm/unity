/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.html.Span;

import static io.imunity.vaadin.elements.CssClassNames.*;

public class InputLabel extends Span
{
	private boolean required;

	public InputLabel(String label)
	{
		setText(label);
		addClassName("u-input-label");
		addClassName(NO_PADDING_TOP.getName());
	}

	@Override
	public void setText(String text)
	{
		super.setText(text);
		if(text == null || text.isBlank())
			getStyle().remove("padding-top");
		else
			getStyle().set("padding-top" ,"var(--unity-input-label-top-padding)");
	}

	public void setRequired(boolean required)
	{
		if (required)
		{
			addClassName(INDICATOR.getName());
			this.required = true;
		}
	}

	public void setErrorMode()
	{
		if(required)
		{
			removeClassName(INDICATOR.getName());
			addClassName(ERROR_INDICATOR.getName());
		}
	}

	public void setNormalMode()
	{
		if(required)
		{
			removeClassName(ERROR_INDICATOR.getName());
			addClassName(INDICATOR.getName());
		}
	}
}
