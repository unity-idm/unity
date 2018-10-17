/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.binding;

import com.vaadin.data.Binder;

/**
 * Useful for trivial {@link Binder}s that should operate on a single field of string type
 * @author K. Benedyczak
 */
public class StringBindingValue
{
	private String value;

	public StringBindingValue(String value)
	{
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
}