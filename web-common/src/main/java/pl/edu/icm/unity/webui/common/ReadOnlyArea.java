/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.TextArea;

public class ReadOnlyArea extends TextArea
{
	public ReadOnlyArea(String value, int rows)
	{
		setValue(value);
		setReadOnly(true);
		setRows(rows);
	}
}
