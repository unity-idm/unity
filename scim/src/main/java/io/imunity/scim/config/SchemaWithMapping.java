/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = SchemaWithMapping.Builder.class)
public class SchemaWithMapping
{
	public final String id;
	public final String name;
	public final String description;
	public final boolean enable;
	public final List<AttributeDefinitionWithMapping> attributesWithMapping;

	private SchemaWithMapping(Builder builder)
	{
		this.id = builder.id;
		this.name = builder.name;
		this.description = builder.description;
		this.enable = builder.enable;
		this.attributesWithMapping = List.copyOf(builder.attributes);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributesWithMapping, description, id, name, enable);
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
		SchemaWithMapping other = (SchemaWithMapping) obj;
		return Objects.equals(attributesWithMapping, other.attributesWithMapping)
				&& Objects.equals(description, other.description) && Objects.equals(id, other.id)
				&& Objects.equals(name, other.name) && Objects.equals(enable, other.enable);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String id;
		private String name;
		private String description;
		private boolean enable;
		private List<AttributeDefinitionWithMapping> attributes = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withId(String id)
		{
			this.id = id;
			return this;
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

		public Builder withEnable(boolean enable)
		{
			this.enable = enable;
			return this;
		}

		public Builder withAttributesWithMapping(List<AttributeDefinitionWithMapping> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		public SchemaWithMapping build()
		{
			return new SchemaWithMapping(this);
		}
	}

}
