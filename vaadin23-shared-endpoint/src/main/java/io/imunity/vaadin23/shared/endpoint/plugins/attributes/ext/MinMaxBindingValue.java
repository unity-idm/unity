/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.shared.endpoint.plugins.attributes.ext;

/**
 * Helper for vaadin binding
 * @author P.Piernik
 *
 * @param <T>
 */
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
