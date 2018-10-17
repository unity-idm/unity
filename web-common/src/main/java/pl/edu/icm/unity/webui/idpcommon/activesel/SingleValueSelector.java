/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon.activesel;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.Lists;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.RadioButtonGroup;

import pl.edu.icm.unity.types.basic.DynamicAttribute;

/**
 * Selection of a single value of a {@link DynamicAttribute}
 * 
 * @author K. Benedyczak
 */
class SingleValueSelector extends CustomComponent implements ValueSelector
{
	private RadioButtonGroup<ValueItem> group;
	
	SingleValueSelector(String name, List<String> values)
	{
		group = new RadioButtonGroup<>();
		List<ValueItem> items = IntStream.range(0, values.size())
			.mapToObj(i -> new ValueItem(values.get(i), i))
			.collect(Collectors.toList());
		group.setItems(items);
		group.setItemCaptionGenerator(i -> i.value);
		group.setSelectedItem(items.get(0));
		setCaption(name);
		setCompositionRoot(group);
		addStyleName("u-activeValueSingleSelect");
	}
	
	@Override
	public List<Integer> getSelectedValueIndices()
	{
		return Lists.newArrayList(group.getSelectedItem().get().index);
	}
	
	static class ValueItem
	{
		final String value;
		final int index;
		
		ValueItem(String value, int index)
		{
			this.value = value;
			this.index = index;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + index;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ValueItem other = (ValueItem) obj;
			if (index != other.index)
				return false;
			if (value == null)
			{
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
	}
}
