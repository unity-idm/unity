/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestGroupMember.Builder.class)
public class RestGroupMember
{
	public final String group;
	public final RestEntity entity;
	public final Collection<RestAttributeExt> attributes;

	private RestGroupMember(Builder builder)
	{
		this.group = builder.group;
		this.entity = builder.entity;
		this.attributes = Optional.ofNullable(builder.attributes)
				.map(List::copyOf)
				.orElse(null);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributes, entity, group);
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
		RestGroupMember other = (RestGroupMember) obj;
		return Objects.equals(attributes, other.attributes) && Objects.equals(entity, other.entity)
				&& Objects.equals(group, other.group);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String group;
		private RestEntity entity;
		private Collection<RestAttributeExt> attributes = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withGroup(String group)
		{
			this.group = group;
			return this;
		}

		public Builder withEntity(RestEntity entity)
		{
			this.entity = entity;
			return this;
		}

		public Builder withAttributes(Collection<RestAttributeExt> attributes)
		{
			this.attributes = Optional.ofNullable(attributes)
					.map(List::copyOf)
					.orElse(null);
			return this;
		}

		public RestGroupMember build()
		{
			return new RestGroupMember(this);
		}
	}

}
