/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema.resourceType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.imunity.scim.common.BasicSCIMResource;

public class SCIMResourceTypeResource extends BasicSCIMResource
{
	static final String SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:ResourceType";

	public final String id;
	public final String name;
	public final String description;
	public final String endpoint;
	public final String schema;
	public final List<SchemaExtension> schemaExtensions;

	private SCIMResourceTypeResource(Builder builder)
	{
		super(builder);
		this.id = builder.id;
		this.name = builder.name;
		this.description = builder.description;
		this.endpoint = builder.endpoint;
		this.schema = builder.schema;
		this.schemaExtensions = builder.schemaExtensions;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(description, endpoint, id, name, schema, schemaExtensions);
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
		SCIMResourceTypeResource other = (SCIMResourceTypeResource) obj;
		return Objects.equals(description, other.description) && Objects.equals(endpoint, other.endpoint)
				&& Objects.equals(id, other.id) && Objects.equals(name, other.name)
				&& Objects.equals(schema, other.schema) && Objects.equals(schemaExtensions, other.schemaExtensions);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends BasicScimResourceBuilder<Builder>
	{
		private String id;
		private String name;
		private String description;
		private String endpoint;
		private String schema;
		private List<SchemaExtension> schemaExtensions = Collections.emptyList();

		private Builder()
		{
			withSchemas(Set.of(SCHEMA));
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

		public Builder withEndpoint(String endpoint)
		{
			this.endpoint = endpoint;
			return this;
		}

		public Builder withSchema(String schema)
		{
			this.schema = schema;
			return this;
		}

		public Builder withSchemaExtensions(List<SchemaExtension> schemaExtensions)
		{
			this.schemaExtensions = schemaExtensions;
			return this;
		}

		public SCIMResourceTypeResource build()
		{
			return new SCIMResourceTypeResource(this);
		}
	}

	public static class SchemaExtension
	{
		public final String schema;
		public final boolean required;

		private SchemaExtension(Builder builder)
		{
			this.schema = builder.schema;
			this.required = builder.required;
		}

		public static Builder builder()
		{
			return new Builder();
		}

		public static final class Builder
		{
			private String schema;
			private boolean required;

			private Builder()
			{
			}

			public Builder withSchema(String schema)
			{
				this.schema = schema;
				return this;
			}

			public Builder withRequired(boolean required)
			{
				this.required = required;
				return this;
			}

			public SchemaExtension build()
			{
				return new SchemaExtension(this);
			}
		}

	}
}
