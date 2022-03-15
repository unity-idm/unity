/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DataArray.Builder.class)
public class DataArray
{
	public enum DataArrayType
	{
		ATTRIBUTE, IDENTITY, MEMBERSHIP
	}

	public final DataArrayType type;
	public final Optional<String> value;

	private DataArray(Builder builder)
	{
		this.type = builder.type;
		this.value = Optional.ofNullable(builder.value);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private DataArrayType type;
		private String value;

		private Builder()
		{
		}

		public Builder withType(DataArrayType type)
		{
			this.type = type;
			return this;
		}

		public Builder withValue(String value)
		{
			this.value = value;
			return this;
		}

		public DataArray build()
		{
			return new DataArray(this);
		}
	}

}