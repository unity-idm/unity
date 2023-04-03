/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.ac;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBAttributesClass.Builder.class)
class DBAttributesClass
{
	public final String name;
	public final String description;
	public final Set<String> allowed;
	public final Set<String> mandatory;
	public final boolean allowArbitrary;
	public final Set<String> parentClasses;

	private DBAttributesClass(Builder builder)
	{
		this.name = builder.name;
		this.description = builder.description;
		this.allowed = builder.allowed;
		this.mandatory = builder.mandatory;
		this.allowArbitrary = builder.allowArbitrary;
		this.parentClasses = builder.parentClasses;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(allowArbitrary, allowed, description, mandatory, name, parentClasses);
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
		DBAttributesClass other = (DBAttributesClass) obj;
		return allowArbitrary == other.allowArbitrary && Objects.equals(allowed, other.allowed)
				&& Objects.equals(description, other.description) && Objects.equals(mandatory, other.mandatory)
				&& Objects.equals(name, other.name) && Objects.equals(parentClasses, other.parentClasses);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private String description;
		private Set<String> allowed = Collections.emptySet();
		private Set<String> mandatory = Collections.emptySet();
		private boolean allowArbitrary;
		private Set<String> parentClasses = Collections.emptySet();

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

		public Builder withAllowed(Set<String> allowed)
		{
			this.allowed = allowed;
			return this;
		}

		public Builder withMandatory(Set<String> mandatory)
		{
			this.mandatory = mandatory;
			return this;
		}

		public Builder withAllowArbitrary(boolean allowArbitrary)
		{
			this.allowArbitrary = allowArbitrary;
			return this;
		}

		public Builder withParentClasses(Set<String> parentClasses)
		{
			this.parentClasses = parentClasses;
			return this;
		}

		public DBAttributesClass build()
		{
			return new DBAttributesClass(this);
		}
	}
}
