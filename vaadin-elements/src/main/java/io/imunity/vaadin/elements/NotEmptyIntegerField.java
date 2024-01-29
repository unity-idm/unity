/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.textfield.IntegerField;

public class NotEmptyIntegerField extends IntegerField
{
	public NotEmptyIntegerField()
	{
		init();
	}

	public NotEmptyIntegerField(String label)
	{
		super(label);
		init();
	}

	private void init()
	{
		addValueChangeListener(event ->
		{
			if(event.getValue() == null || event.getValue() > getMax() || event.getValue() < getMin())
				if(event.getOldValue() != null)
					setValue(event.getOldValue());
		});
	}
}
