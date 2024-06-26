/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.common;

import static org.springframework.util.Assert.notEmpty;
import static org.springframework.util.Assert.notNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonPropertyOrder(
{ "schemas", "totalResults", "resources" })
@JsonDeserialize(builder = ListResponse.Builder.class)
public class ListResponse<T extends BasicSCIMResource>
{
	public static final String SCHEMA = "urn:ietf:params:scim:api:messages:2.0:ListResponse";

	public final Set<String> schemas;
	public final int totalResults;
	public final List<T> resources;

	private ListResponse(Builder<T> builder)
	{
		this.schemas = Set.copyOf(builder.schemas);
		this.totalResults = builder.totalResults;
		this.resources = List.copyOf(builder.resources);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(resources, schemas, totalResults);
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
		ListResponse<?> other = (ListResponse<?>) obj;
		return Objects.equals(resources, other.resources) && Objects.equals(schemas, other.schemas)
				&& totalResults == other.totalResults;
	}

	public static <T extends BasicSCIMResource> Builder<T> builder()
	{
		return new Builder<T>();
	}

	public static final class Builder<K extends BasicSCIMResource>
	{
		private Set<String> schemas;
		private Integer totalResults;
		private List<K> resources = Collections.emptyList();

		private Builder()
		{
			withSchemas(Set.of(SCHEMA));
		}

		private Builder<K> withSchemas(Set<String> schemas)
		{
			this.schemas = schemas;
			return this;
		}

		public Builder<K> withTotalResults(int totalResults)
		{
			this.totalResults = totalResults;
			return this;
		}

		public Builder<K> withResources(List<K> resources)
		{
			this.resources = resources;
			return this;
		}

		public ListResponse<K> build()
		{
			notNull(schemas, "schemas cannot be null.");
			notEmpty(schemas, "schemas cannot be empty.");
			notNull(totalResults, "id cannot be null.");
			return new ListResponse<K>(this);
		}
	}

}
