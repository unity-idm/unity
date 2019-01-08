/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon.activesel;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.CustomComponent;

import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.webui.idpcommon.activesel.SingleValueSelector.ValueItem;

/**
 * Selection of a multiple values of a {@link DynamicAttribute}
 * 
 * @author K. Benedyczak
 */
class MultiValueSelector extends CustomComponent implements ValueSelector
{
	private CheckBoxGroup<ValueItem> group;
	
	MultiValueSelector(String name, List<String> values)
	{
		group = new CheckBoxGroup<>();
		List<ValueItem> items = IntStream.range(0, values.size())
			.mapToObj(i -> new ValueItem(values.get(i), i))
			.collect(Collectors.toList());
		group.setItems(items);
		group.setItemCaptionGenerator(i -> i.value);
		setCaption(name);
		setCompositionRoot(group);
		addStyleName("u-activeValueMultiSelect");
	}
	
	@Override
	public List<Integer> getSelectedValueIndices()
	{
		return group.getSelectedItems().stream().map(i -> i.index).collect(Collectors.toList());
	}
}
