/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.message_templates;

import com.vaadin.flow.component.HasValue;

class FocusedField
{
	private HasValue<?, String> field;
	private int cursorPosition;

	void set(HasValue<?, String> field, int cursorPosition)
	{
		this.field = field;
		this.cursorPosition = cursorPosition;
	}

	boolean isSet()
	{
		return field != null;
	}

	String getValue()
	{
		return field.getValue();
	}

	void setValue(String value)
	{
		field.setValue(value);
	}

	int getCursorPosition()
	{
		return cursorPosition;
	}
}
