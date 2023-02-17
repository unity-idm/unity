/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.checkbox.CheckboxGroup;

import java.util.Set;

public class CheckboxWithError extends CheckboxGroup<String>
{
	private final String label;
	public CheckboxWithError(String label)
	{
		super(null, Set.of(label));
		this.label = label;
		addValueChangeListener(e -> setInvalid(false));
	}

	public boolean getState()
	{
		return getValue().contains(label);
	}

	public void setValue(boolean value)
	{
		if(value)
			setValue(Set.of(label));
		else
			deselect(label);
	}
}
