/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.bounded_editors;

import java.util.Objects;

public class ValueWrapper
{
	private String value;
	private boolean unlimited;

	ValueWrapper(String value, boolean unlimited)
	{
		this.value = value;
		this.unlimited = unlimited;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public boolean isUnlimited()
	{
		return unlimited;
	}

	public void setUnlimited(boolean unlimited)
	{
		this.unlimited = unlimited;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(unlimited, value);
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
		ValueWrapper other = (ValueWrapper) obj;
		return unlimited == other.unlimited && Objects.equals(value, other.value);
	}
	
	
}