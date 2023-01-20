/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.components;

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