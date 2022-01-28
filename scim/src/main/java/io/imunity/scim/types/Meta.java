/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.types;

import java.net.URI;

import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = Meta.Builder.class)
public class Meta
{
	public enum ResourceType
	{
		User, Group
	}

	public final ResourceType resourceType;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	public final Instant created;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	public final Instant lastModified;
	public final URI location;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public final String version;

	private Meta(Builder builder)
	{
		this.resourceType = builder.resourceType;
		this.created = builder.created;
		this.lastModified = builder.lastModified;
		this.location = builder.location;
		this.version = builder.version;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(created, lastModified, location, resourceType, version);
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
		Meta other = (Meta) obj;
		return Objects.equals(created, other.created) && Objects.equals(lastModified, other.lastModified)
				&& Objects.equals(location, other.location) && resourceType == other.resourceType
				&& Objects.equals(version, other.version);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private ResourceType resourceType;
		private Instant created;
		private Instant lastModified;
		private URI location;
		private String version;

		private Builder()
		{
		}

		public Builder withResourceType(ResourceType resourceType)
		{
			this.resourceType = resourceType;
			return this;
		}

		public Builder withCreated(Instant created)
		{
			this.created = created;
			return this;
		}

		public Builder withLastModified(Instant lastModified)
		{
			this.lastModified = lastModified;
			return this;
		}

		public Builder withLocation(URI location)
		{
			this.location = location;
			return this;
		}

		public Builder withVersion(String version)
		{
			this.version = version;
			return this;
		}

		public Meta build()
		{
			return new Meta(this);
		}
	}

}
