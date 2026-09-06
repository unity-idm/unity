/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.rest.api;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestAttributeCacheValidationReport.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RestAttributeCacheValidationReport
{
	public final List<RestGroupValidationReport> groups;

	private RestAttributeCacheValidationReport(Builder builder)
	{
		this.groups = List.copyOf(builder.groups);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(groups);
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
		RestAttributeCacheValidationReport other = (RestAttributeCacheValidationReport) obj;
		return Objects.equals(groups, other.groups);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private List<RestGroupValidationReport> groups = List.of();

		private Builder()
		{
		}

		public Builder withGroups(List<RestGroupValidationReport> groups)
		{
			this.groups = groups;
			return this;
		}

		public RestAttributeCacheValidationReport build()
		{
			return new RestAttributeCacheValidationReport(this);
		}
	}
}
