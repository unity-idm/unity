/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.HasValue;

public class FocusedField
{
	private HasValue<?, String> field;
	private int cursorPosition;

	public void set(HasValue<?, String> field, int cursorPosition)
	{
		this.field = field;
		this.cursorPosition = cursorPosition;
	}

	public boolean isSet()
	{
		return field != null;
	}

	public String getValue()
	{
		return field.getValue();
	}

	public String getLocale()
	{
		return ((LocalizedTextField)field).locale.getLanguage();
	}

	public void setValue(String value)
	{
		field.setValue(value);
	}

	public int getCursorPosition()
	{
		return cursorPosition;
	}
}
