/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.active_value_select;

import java.util.Objects;

class ValueItem
{
	final String value;
	final int index;

	ValueItem(String value, int index)
	{
		this.value = value;
		this.index = index;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ValueItem valueItem = (ValueItem) o;
		return index == valueItem.index && Objects.equals(value, valueItem.value);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(value, index);
	}

	@Override
	public String toString()
	{
		return "ValueItem{" +
				"value='" + value + '\'' +
				", index=" + index +
				'}';
	}
}
