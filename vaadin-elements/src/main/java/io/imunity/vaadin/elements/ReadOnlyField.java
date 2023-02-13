/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;


import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.textfield.TextField;

public class ReadOnlyField extends TextField
{
	public ReadOnlyField(String value, float width, Unit widthUnit)
	{
		setValue(value);
		setWidth(width, widthUnit);
		setReadOnly(true);
	}

	public ReadOnlyField(String value)
	{
		setValue(value);
	}
}
