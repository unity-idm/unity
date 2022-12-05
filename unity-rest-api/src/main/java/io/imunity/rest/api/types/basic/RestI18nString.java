/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;

@JsonDeserialize(builder = RestI18nString.Builder.class)
public class RestI18nString
{
	@JsonProperty("Map")
	public final Map<String, String> values;
	@JsonProperty("DefaultValue")
	public final String defaultValue;

	private RestI18nString(Builder builder)
	{
		this.values = builder.values;
		this.defaultValue = builder.defaultValue;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(defaultValue, values);
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
		RestI18nString other = (RestI18nString) obj;
		return Objects.equals(defaultValue, other.defaultValue) && Objects.equals(values, other.values);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		@JsonProperty("Map")
		private Map<String, String> values = Collections.emptyMap();
		@JsonProperty("DefaultValue")
		private String defaultValue;

		private Builder()
		{
		}

		public Builder withValues(Map<String, String> values)
		{
			this.values = values;
			return this;
		}

		public Builder withDefaultValue(String defaultValue)
		{
			this.defaultValue = defaultValue;
			return this;
		}

		public RestI18nString build()
		{
			return new RestI18nString(this);
		}
	}

}
