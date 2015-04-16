/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.TextArea;

/**
 * {@link TextArea} specialization with settings common for description areas.
 * @author K. Benedyczak
 */
public class DescriptionTextArea extends TextArea
{
	/**
	 * Editable, without a value, no caption
	 * @param caption
	 */
	public DescriptionTextArea()
	{
		this("", "");
	}

	/**
	 * Editable, without a value
	 * @param caption
	 */
	public DescriptionTextArea(String caption)
	{
		this(caption, "");
	}

	/**
	 * Allows to set whether is read only and the initial value
	 * @param caption
	 * @param readOnly
	 * @param initialValue
	 */
	public DescriptionTextArea(String caption, String initialValue)
	{
		super(caption);
		init(initialValue);
	}
	
	protected void init(String initialValue)
	{
		setWordwrap(true);
		setWidth(100, Unit.PERCENTAGE);
		setValue(initialValue);
		addStyleName(Styles.vBorderLess.toString());
	}
	
	@Override
	public void setValue(String value)
	{
		super.setValue(value);
		int len = value.length();
		if (len < 40)
			setRows(1);
		else if (len < 80)
			setRows(2);
		else if (len < 300)
			setRows(3);
		else
			setRows(4);
	}
}
