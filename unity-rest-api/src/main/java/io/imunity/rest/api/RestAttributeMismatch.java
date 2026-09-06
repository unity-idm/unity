/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.rest.api;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestAttributeMismatch.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RestAttributeMismatch
{
	public final String attributeName;
	public final List<String> cachedValues;
	public final List<String> freshValues;

	private RestAttributeMismatch(Builder builder)
	{
		this.attributeName = builder.attributeName;
		this.cachedValues = List.copyOf(builder.cachedValues);
		this.freshValues = List.copyOf(builder.freshValues);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributeName, cachedValues, freshValues);
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
		RestAttributeMismatch other = (RestAttributeMismatch) obj;
		return Objects.equals(attributeName, other.attributeName)
				&& Objects.equals(cachedValues, other.cachedValues)
				&& Objects.equals(freshValues, other.freshValues);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String attributeName;
		private List<String> cachedValues = List.of();
		private List<String> freshValues = List.of();

		private Builder()
		{
		}

		public Builder withAttributeName(String attributeName)
		{
			this.attributeName = attributeName;
			return this;
		}

		public Builder withCachedValues(List<String> cachedValues)
		{
			this.cachedValues = cachedValues;
			return this;
		}

		public Builder withFreshValues(List<String> freshValues)
		{
			this.freshValues = freshValues;
			return this;
		}

		public RestAttributeMismatch build()
		{
			return new RestAttributeMismatch(this);
		}
	}
}
