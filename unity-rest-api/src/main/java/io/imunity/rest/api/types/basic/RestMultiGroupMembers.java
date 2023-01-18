/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;

@JsonDeserialize(builder = RestMultiGroupMembers.Builder.class)
public class RestMultiGroupMembers
{
	public final Collection<RestEntity> entities;
	public final Map<String, List<RestEntityGroupAttributes>> members;

	private RestMultiGroupMembers(Builder builder)
	{
		this.entities = builder.entities;
		this.members = builder.members;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entities, members);
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
		RestMultiGroupMembers other = (RestMultiGroupMembers) obj;
		return Objects.equals(entities, other.entities) && Objects.equals(members, other.members);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private Collection<RestEntity> entities = Collections.emptyList();
		private Map<String, List<RestEntityGroupAttributes>> members = Collections.emptyMap();

		private Builder()
		{
		}

		public Builder withEntities(Collection<RestEntity> entities)
		{
			this.entities = entities;
			return this;
		}

		public Builder withMembers(Map<String, List<RestEntityGroupAttributes>> members)
		{
			this.members = members;
			return this;
		}

		public RestMultiGroupMembers build()
		{
			return new RestMultiGroupMembers(this);
		}
	}

}
