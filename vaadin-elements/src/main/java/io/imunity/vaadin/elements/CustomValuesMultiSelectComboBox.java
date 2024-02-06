/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.dataview.ComboBoxListDataView;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomValuesMultiSelectComboBox extends MultiSelectComboBox<String>
{

	private Set<String> items;

	public CustomValuesMultiSelectComboBox(String label)
	{
		this();
		setLabel(label);
	}

	public CustomValuesMultiSelectComboBox()
	{
		items = new HashSet<>();
		setAllowCustomValue(true);
		addCustomValueSetListener(event ->
		{
			HashSet<String> values = new HashSet<>(getValue());
			values.add(event.getDetail());
			setValue(values);
		});
	}

	@Override
	public ComboBoxListDataView<String> setItems(Collection<String> items)
	{
		ComboBoxListDataView<String> stringComboBoxListDataView = super.setItems(items);
		this.items = stringComboBoxListDataView.getItems().collect(Collectors.toCollection(LinkedHashSet::new));
		return stringComboBoxListDataView;
	}

	@Override
	public ComboBoxListDataView<String> setItems(String... items)
	{
		ComboBoxListDataView<String> stringComboBoxListDataView = super.setItems(items);
		this.items = stringComboBoxListDataView.getItems().collect(Collectors.toSet());
		return stringComboBoxListDataView;
	}

	@Override
	public void setValue(Set<String> values)
	{
		if(values == null)
			return;
		items.addAll(values);
		setItems(items);
		super.setValue(values);
	}
}
