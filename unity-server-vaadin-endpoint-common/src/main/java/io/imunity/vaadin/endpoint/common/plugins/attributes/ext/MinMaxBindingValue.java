/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;


class MinMaxBindingValue<T>
{
	private T min;
	private T max;

	T getMin()
	{
		return min;
	}
	void setMin(T min)
	{
		this.min = min;
	}
	T getMax()
	{
		return max;
	}
	void setMax(T max)
	{
		this.max = max;
	}	
}
