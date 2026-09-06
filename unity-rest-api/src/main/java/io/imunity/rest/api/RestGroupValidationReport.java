/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.rest.api;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestGroupValidationReport.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RestGroupValidationReport
{
	public final String group;
	public final List<Long> entitiesWithPendingUpdate;
	public final List<RestEntityAttributeMismatches> entitiesWithMismatches;

	private RestGroupValidationReport(Builder builder)
	{
		this.group = builder.group;
		this.entitiesWithPendingUpdate = List.copyOf(builder.entitiesWithPendingUpdate);
		this.entitiesWithMismatches = List.copyOf(builder.entitiesWithMismatches);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(group, entitiesWithPendingUpdate, entitiesWithMismatches);
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
		RestGroupValidationReport other = (RestGroupValidationReport) obj;
		return Objects.equals(group, other.group)
				&& Objects.equals(entitiesWithPendingUpdate, other.entitiesWithPendingUpdate)
				&& Objects.equals(entitiesWithMismatches, other.entitiesWithMismatches);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String group;
		private List<Long> entitiesWithPendingUpdate = List.of();
		private List<RestEntityAttributeMismatches> entitiesWithMismatches = List.of();

		private Builder()
		{
		}

		public Builder withGroup(String group)
		{
			this.group = group;
			return this;
		}

		public Builder withEntitiesWithPendingUpdate(List<Long> entitiesWithPendingUpdate)
		{
			this.entitiesWithPendingUpdate = entitiesWithPendingUpdate;
			return this;
		}

		public Builder withEntitiesWithMismatches(List<RestEntityAttributeMismatches> entitiesWithMismatches)
		{
			this.entitiesWithMismatches = entitiesWithMismatches;
			return this;
		}

		public RestGroupValidationReport build()
		{
			return new RestGroupValidationReport(this);
		}
	}
}
