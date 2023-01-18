/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestEntityGroupAttributes.Builder.class)
public class RestEntityGroupAttributes
{
	public final long entityId;
	public final Collection<RestAttributeExt> attributes;

	private RestEntityGroupAttributes(Builder builder)
	{
		this.entityId = builder.entityId;
		this.attributes = builder.attributes;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributes, entityId);
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
		RestEntityGroupAttributes other = (RestEntityGroupAttributes) obj;
		return Objects.equals(attributes, other.attributes) && entityId == other.entityId;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private long entityId;
		private Collection<RestAttributeExt> attributes = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withEntityId(long entityId)
		{
			this.entityId = entityId;
			return this;
		}

		public Builder withAttributes(Collection<RestAttributeExt> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		public RestEntityGroupAttributes build()
		{
			return new RestEntityGroupAttributes(this);
		}
	}
}
