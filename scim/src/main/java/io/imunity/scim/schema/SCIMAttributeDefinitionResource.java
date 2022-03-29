/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema;

import java.util.Collection;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = SCIMAttributeDefinitionResource.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class SCIMAttributeDefinitionResource
{
	public final String name;
	public final String type;
	public final String description;
	public final List<SCIMAttributeDefinitionResource> subAttributes;
	public final boolean required;
	public final List<String> canonicalValues;
	public final String mutability;
	public final boolean multiValued;
	public final boolean caseExact;
	public final String uniqueness;
	public final String returned;
	public final List<String> referenceTypes;

	private SCIMAttributeDefinitionResource(Builder builder)
	{
		this.name = builder.name;
		this.type = builder.type;
		this.description = builder.description;
		this.subAttributes = List.copyOf(builder.subAttributes);
		this.required = builder.required;
		this.canonicalValues = List.copyOf(builder.canonicalValues);
		this.mutability = builder.mutability;
		this.multiValued = builder.multiValued;
		this.caseExact = builder.caseExact;
		this.uniqueness = builder.uniqueness;
		this.returned = builder.returned;
		this.referenceTypes = List.copyOf(builder.referenceTypes);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(canonicalValues, caseExact, description, multiValued, mutability, name, referenceTypes,
				required, returned, subAttributes, type, uniqueness);
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
		SCIMAttributeDefinitionResource other = (SCIMAttributeDefinitionResource) obj;
		return Objects.equals(canonicalValues, other.canonicalValues) && caseExact == other.caseExact
				&& Objects.equals(description, other.description) && multiValued == other.multiValued
				&& Objects.equals(mutability, other.mutability) && Objects.equals(name, other.name)
				&& Objects.equals(referenceTypes, other.referenceTypes) && required == other.required
				&& Objects.equals(returned, other.returned) && Objects.equals(subAttributes, other.subAttributes)
				&& Objects.equals(type, other.type) && Objects.equals(uniqueness, other.uniqueness);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder
	{
		private String name;
		private String type;
		private String description;
		private Collection<SCIMAttributeDefinitionResource> subAttributes = Collections.emptyList();
		private boolean required;
		private Collection<String> canonicalValues = Collections.emptyList();
		private String mutability;
		private boolean multiValued;
		private boolean caseExact;
		private String uniqueness;
		private String returned;
		private Collection<String> referenceTypes = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withType(String type)
		{
			this.type = type;
			return this;
		}

		public Builder withDescription(String description)
		{
			this.description = description;
			return this;
		}

		public Builder withSubAttributes(Collection<SCIMAttributeDefinitionResource> subAttributes)
		{
			this.subAttributes = subAttributes;
			return this;
		}

		public Builder withRequired(boolean required)
		{
			this.required = required;
			return this;
		}

		public Builder withCanonicalValues(Collection<String> canonicalValues)
		{
			this.canonicalValues = canonicalValues;
			return this;
		}

		public Builder withMutability(String mutability)
		{
			this.mutability = mutability;
			return this;
		}

		public Builder withMultiValued(boolean multiValued)
		{
			this.multiValued = multiValued;
			return this;
		}

		public Builder withCaseExact(boolean caseExact)
		{
			this.caseExact = caseExact;
			return this;
		}

		public Builder withUniqueness(String uniqueness)
		{
			this.uniqueness = uniqueness;
			return this;
		}

		public Builder withReturned(String returned)
		{
			this.returned = returned;
			return this;
		}

		public Builder withReferenceTypes(Collection<String> referenceTypes)
		{
			this.referenceTypes = referenceTypes;
			return this;
		}

		public SCIMAttributeDefinitionResource build()
		{
			return new SCIMAttributeDefinitionResource(this);
		}
	}
}
