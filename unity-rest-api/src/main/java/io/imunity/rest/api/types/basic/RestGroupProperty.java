/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestGroupProperty.Builder.class)

public class RestGroupProperty
{
	public final String key;
	public final String value;

	private RestGroupProperty(Builder builder)
	{
		this.key = builder.key;
		this.value = builder.value;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(key, value);
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
		RestGroupProperty other = (RestGroupProperty) obj;
		return Objects.equals(key, other.key) && Objects.equals(value, other.value);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String key;
		private String value;

		private Builder()
		{
		}

		public Builder withKey(String key)
		{
			this.key = key;
			return this;
		}

		public Builder withValue(String value)
		{
			this.value = value;
			return this;
		}

		public RestGroupProperty build()
		{
			return new RestGroupProperty(this);
		}
	}

}
