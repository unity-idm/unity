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
public class DescriptionTextArea2 extends TextArea
{
	/**
	 * Editable, without a value, no caption
	 * @param caption
	 */
	public DescriptionTextArea2()
	{
		this("", "");
	}

	/**
	 * Editable, without a value
	 * @param caption
	 */
	public DescriptionTextArea2(String caption)
	{
		this(caption, "");
	}

	/**
	 * Allows to set whether is read only and the initial value
	 * @param caption
	 * @param readOnly
	 * @param initialValue
	 */
	public DescriptionTextArea2(String caption, String initialValue)
	{
		super(caption);
		init(initialValue);
	}
	
	protected void init(String initialValue)
	{
		setWordWrap(true);
		setWidth(100, Unit.PERCENTAGE);
		setValue(initialValue);
		setRows(3);
	}
}
