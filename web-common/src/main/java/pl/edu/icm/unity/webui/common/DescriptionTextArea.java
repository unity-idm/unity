/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.TextArea;

/**
 * {@link TextArea} specialization with settings common for description areas.
 * If it has an empty string set as a value, the component becomes automatically invisible (and vice versa).
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
		this(false, "");
	}

	/**
	 * Allows to set whether is read only and the initial value, no caption
	 * @param caption
	 * @param readOnly
	 * @param initialValue
	 */
	public DescriptionTextArea(boolean readOnly, String initialValue)
	{
		setWordwrap(true);
		setWidth(100, Unit.PERCENTAGE);
		setValue(initialValue);
		setReadOnly(readOnly);
	}
	
	/**
	 * Editable, without a value
	 * @param caption
	 */
	public DescriptionTextArea(String caption)
	{
		this(caption, false, "");
	}

	/**
	 * Allows to set whether is read only and the initial value
	 * @param caption
	 * @param readOnly
	 * @param initialValue
	 */
	public DescriptionTextArea(String caption, boolean readOnly, String initialValue)
	{
		super(caption);
		setWordwrap(true);
		setWidth(100, Unit.PERCENTAGE);
		setValue(initialValue);
		setReadOnly(readOnly);
	}
	
	@Override
	public void setValue(String value)
	{
		boolean ro = isReadOnly();
		if (ro)
			setReadOnly(false);
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
		if (ro)
			setReadOnly(true);
		if (value.equals(""))
			setVisible(false);
		else
			setVisible(true);
	}
}
