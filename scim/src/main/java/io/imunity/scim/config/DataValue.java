/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DataValue.Builder.class)
public class DataValue
{
	public enum DataValueType
	{
		ATTRIBUTE, IDENTITY, ARRAY, MVEL
	}

	public final DataValueType type;
	public final Optional<String> value;

	private DataValue(Builder builder)
	{
		this.type = builder.type;
		this.value = Optional.ofNullable(builder.value);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(type, value);
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
		DataValue other = (DataValue) obj;
		return type == other.type && Objects.equals(value, other.value);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private DataValueType type;
		private String value;

		private Builder()
		{
			type = DataValueType.MVEL;
		}

		public Builder withType(DataValueType type)
		{
			this.type = type;
			return this;
		}

		public Builder withValue(String value)
		{
			this.value = value;
			return this;
		}

		public DataValue build()
		{
			return new DataValue(this);
		}
	}

}