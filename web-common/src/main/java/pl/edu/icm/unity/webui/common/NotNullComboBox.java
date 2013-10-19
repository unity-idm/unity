/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.data.Item;
import com.vaadin.ui.ComboBox;

/**
 * Simple {@link ComboBox} which doesn't allow for empty selections, unless there is no contents.
 * 
 * @author K. Benedyczak
 */
public class NotNullComboBox extends ComboBox
{
	public NotNullComboBox(String caption)
	{
		super(caption);
		init();
	}

	protected final void init()
	{
		setNullSelectionAllowed(false);
	}
	
	public Item addItem(Object item)
	{
		Item ret = super.addItem(item);
		if (size() == 1)
			setValue(item);
		return ret;
	}
}
