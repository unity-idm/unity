/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;


public class MinMaxBindingValue<T>
{
	private T min;
	private T max;

	public T getMin()
	{
		return min;
	}
	public void setMin(T min)
	{
		this.min = min;
	}
	public T getMax()
	{
		return max;
	}
	public void setMax(T max)
	{
		this.max = max;
	}	
}
