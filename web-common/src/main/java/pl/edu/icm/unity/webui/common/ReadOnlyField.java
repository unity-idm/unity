/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.TextField;

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
		setReadOnly(true);
	}
}
