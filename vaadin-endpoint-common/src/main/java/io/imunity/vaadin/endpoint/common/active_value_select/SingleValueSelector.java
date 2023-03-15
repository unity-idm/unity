/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.active_value_select;

import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class SingleValueSelector extends RadioButtonGroup<ValueItem> implements ValueSelector
{
	SingleValueSelector(String name, List<String> values)
	{
		List<ValueItem> items = IntStream.range(0, values.size())
			.mapToObj(i -> new ValueItem(values.get(i), i))
			.collect(Collectors.toList());
		setItems(items);
		setItemLabelGenerator(i -> i.value);
		setValue(items.get(0));
		setLabel(name);

		addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
		addClassName("u-activeValueSingleSelect");
	}
	
	@Override
	public List<Integer> getSelectedValueIndices()
	{
		return List.of(getValue().index);
	}

}
