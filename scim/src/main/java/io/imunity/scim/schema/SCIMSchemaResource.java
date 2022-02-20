/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.scim.common.BasicSCIMResource;

@JsonDeserialize(builder = SCIMSchemaResource.Builder.class)
class SCIMSchemaResource extends BasicSCIMResource
{
	public final String name;
	public final String description;
	public final List<SCIMAttributeDefinitionResource> attributes;

	private SCIMSchemaResource(Builder builder)
	{
		super(builder);
		this.name = builder.name;
		this.description = builder.description;
		this.attributes = List.copyOf(builder.attributes);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(attributes, description, name);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SCIMSchemaResource other = (SCIMSchemaResource) obj;
		return Objects.equals(attributes, other.attributes) && Objects.equals(description, other.description)
				&& Objects.equals(name, other.name);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends BasicScimResourceBuilder<Builder>
	{
		private String name;
		private String description;
		private List<SCIMAttributeDefinitionResource> attributes = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withDescription(String description)
		{
			this.description = description;
			return this;
		}

		public Builder withAttributes(List<SCIMAttributeDefinitionResource> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		public SCIMSchemaResource build()
		{
			return new SCIMSchemaResource(this);
		}
	}

}
