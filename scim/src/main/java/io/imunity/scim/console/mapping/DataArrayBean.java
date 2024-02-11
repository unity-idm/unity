/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console.mapping;

import java.util.Optional;

import io.imunity.scim.config.DataArray.DataArrayType;

public class DataArrayBean
{
	private DataArrayType type;
	private Optional<String> value;

	public DataArrayBean(DataArrayType type, Optional<String> value)
	{
		this.type = type;
		this.value = value;
	}

	public DataArrayBean()
	{
		this(null, Optional.empty());
	}

	public DataArrayType getType()
	{
		return type;
	}

	public void setType(DataArrayType type)
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