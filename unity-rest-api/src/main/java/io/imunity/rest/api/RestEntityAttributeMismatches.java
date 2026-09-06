/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.rest.api;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestEntityAttributeMismatches.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RestEntityAttributeMismatches
{
	public final long entityId;
	public final List<RestAttributeMismatch> mismatches;

	private RestEntityAttributeMismatches(Builder builder)
	{
		this.entityId = builder.entityId;
		this.mismatches = List.copyOf(builder.mismatches);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entityId, mismatches);
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
		RestEntityAttributeMismatches other = (RestEntityAttributeMismatches) obj;
		return entityId == other.entityId && Objects.equals(mismatches, other.mismatches);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private long entityId;
		private List<RestAttributeMismatch> mismatches = List.of();

		private Builder()
		{
		}

		public Builder withEntityId(long entityId)
		{
			this.entityId = entityId;
			return this;
		}

		public Builder withMismatches(List<RestAttributeMismatch> mismatches)
		{
			this.mismatches = mismatches;
			return this;
		}

		public RestEntityAttributeMismatches build()
		{
			return new RestEntityAttributeMismatches(this);
		}
	}
}
