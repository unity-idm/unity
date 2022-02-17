/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.scim.scheme.SCIMAttributeType;

import java.util.Collections;

@JsonDeserialize(builder = AttributeDefinition.Builder.class)
public class AttributeDefinition
{
	public final String name;
	public final SCIMAttributeType type;
	public final String description;
	public final List<AttributeDefinitionWithMapping> subAttributesWithMapping;
	public final boolean multiValued;

	private AttributeDefinition(Builder builder)
	{
		this.name = builder.name;
		this.type = builder.type;
		this.description = builder.description;
		this.subAttributesWithMapping = List.copyOf(builder.subAttributes);
		this.multiValued = builder.multiValued;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(description, multiValued, name, subAttributesWithMapping, type);
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
		AttributeDefinition other = (AttributeDefinition) obj;
		return Objects.equals(description, other.description) && multiValued == other.multiValued
				&& Objects.equals(name, other.name)
				&& Objects.equals(subAttributesWithMapping, other.subAttributesWithMapping) && type == other.type;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private SCIMAttributeType type;
		private String description;
		private List<AttributeDefinitionWithMapping> subAttributes = Collections.emptyList();
		private boolean multiValued;

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withType(SCIMAttributeType type)
		{
			this.type = type;
			return this;
		}

		public Builder withDescription(String description)
		{
			this.description = description;
			return this;
		}

		public Builder withSubAttributesWithMapping(List<AttributeDefinitionWithMapping> subAttributes)
		{
			this.subAttributes = subAttributes;
			return this;
		}

		public Builder withMultiValued(boolean multiValued)
		{
			this.multiValued = multiValued;
			return this;
		}

		public AttributeDefinition build()
		{
			return new AttributeDefinition(this);
		}
	}
}
