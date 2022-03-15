/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.Optional;

import io.imunity.scim.config.DataValue.DataValueType;

public class DataValueBean
{
	private DataValueType type;
	private Optional<String> value;

	public DataValueBean(DataValueType type, Optional<String> value)
	{
		this.type = type;
		this.value = value;
	}

	public DataValueBean()
	{
		this(null, Optional.empty());
	}

	public DataValueType getType()
	{
		return type;
	}

	public void setType(DataValueType type)
	{
		this.type = type;
	}

	public Optional<String> getValue()
	{
		return value;
	}

	public void setValue(Optional<String> value)
	{
		this.value = value;
	}

}