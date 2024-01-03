/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;

import java.util.HashSet;
import java.util.Set;

public class CustomValuesMultiSelectComboBox extends MultiSelectComboBox<String>
{
	public CustomValuesMultiSelectComboBox(String label)
	{
		this();
		setLabel(label);
	}

	public CustomValuesMultiSelectComboBox()
	{
		setAllowCustomValue(true);
		addCustomValueSetListener(event ->
		{
			HashSet<String> values = new HashSet<>(getValue());
			values.add(event.getDetail());
			setItems(values);
			setValue(values);
		});
	}

	@Override
	public void setValue(Set<String> strings)
	{
		if(strings == null)
			return;
		HashSet<String> values = new HashSet<>(getValue());
		values.addAll(strings);
		setItems(values);
		super.setValue(strings);
	}
}
