/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Label;

/**
 * Simple not editable text field
 * 
 * @author P.Piernik
 *
 */
public class LabelAsTextField extends CustomField<String>
{
	private Label content;

	public LabelAsTextField(String caption)
	{
		content = new Label();
		setCaption(caption);
	}

	@Override
	public String getValue()
	{
		return content.getValue();
	}

	@Override
	protected Component initContent()
	{
		return content;
	}

	@Override
	protected void doSetValue(String value)
	{
		content.setValue(value);

	}
}