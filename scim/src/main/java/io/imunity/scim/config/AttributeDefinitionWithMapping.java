/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = AttributeDefinitionWithMapping.Builder.class)
public class AttributeDefinitionWithMapping
{
	public final AttributeDefinition attributeDefinition;
	public final AttributeMapping attributeMapping;

	private AttributeDefinitionWithMapping(Builder builder)
	{
		this.attributeDefinition = builder.attributeDefinition;
		this.attributeMapping = builder.attributeMapping;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributeDefinition, attributeMapping);
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
		AttributeDefinitionWithMapping other = (AttributeDefinitionWithMapping) obj;
		return Objects.equals(attributeDefinition, other.attributeDefinition)
				&& Objects.equals(attributeMapping, other.attributeMapping);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private AttributeDefinition attributeDefinition;
		private AttributeMapping attributeMapping;

		private Builder()
		{
		}

		public Builder withAttributeDefinition(AttributeDefinition attributeDefinition)
		{
			this.attributeDefinition = attributeDefinition;
			return this;
		}

		public Builder withAttributeMapping(AttributeMapping attributeMapping)
		{
			this.attributeMapping = attributeMapping;
			return this;
		}

		public AttributeDefinitionWithMapping build()
		{
			return new AttributeDefinitionWithMapping(this);
		}
	}

}
