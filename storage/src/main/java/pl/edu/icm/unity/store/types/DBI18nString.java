/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.types;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBI18nString.Builder.class)
public class DBI18nString
{
	@JsonProperty("Map")
	public final Map<String, String> values;
	@JsonProperty("DefaultValue")
	public final String defaultValue;

	private DBI18nString(Builder builder)
	{
		this.values = Optional.ofNullable(builder.values)
				.map(HashMap::new)
				.map(Collections::unmodifiableMap)
				.orElse(null);
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
		DBI18nString other = (DBI18nString) obj;
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
			this.values = Optional.ofNullable(values)
					.map(HashMap::new)
					.map(Collections::unmodifiableMap)
					.orElse(null);
			return this;
		}

		public Builder withDefaultValue(String defaultValue)
		{
			this.defaultValue = defaultValue;
			return this;
		}

		public DBI18nString build()
		{
			return new DBI18nString(this);
		}
	}
}
