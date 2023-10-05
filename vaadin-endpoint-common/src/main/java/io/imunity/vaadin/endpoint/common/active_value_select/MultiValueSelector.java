/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.active_value_select;


import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.html.Span;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class MultiValueSelector extends CheckboxGroup<ValueItem> implements ValueSelector
{
	MultiValueSelector(String name, List<String> values)
	{
		List<ValueItem> items = IntStream.range(0, values.size())
			.mapToObj(i -> new ValueItem(values.get(i), i))
			.collect(Collectors.toList());
		setItems(items);
		setItemLabelGenerator(i -> i.value);
		addComponentAsFirst(new Span(name));
		addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
		addClassName("u-activeValueMultiSelect");
	}
	
	@Override
	public List<Integer> getSelectedValueIndices()
	{
		return getSelectedItems().stream()
				.map(i -> i.index)
				.collect(Collectors.toList());
	}
}
