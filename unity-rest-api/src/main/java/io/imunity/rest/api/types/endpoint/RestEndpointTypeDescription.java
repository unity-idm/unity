/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.endpoint;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestEndpointTypeDescription.Builder.class)
public class RestEndpointTypeDescription
{
	public final String name;
	public final String description;
	public final String supportedBinding;
	public final Map<String, String> paths;
	public final Map<String, String> features;

	private RestEndpointTypeDescription(Builder builder)
	{
		this.name = builder.name;
		this.description = builder.description;
		this.supportedBinding = builder.supportedBinding;
		this.paths = Optional.ofNullable(builder.paths)
				.map(Map::copyOf)
				.orElse(null);
		this.features = Optional.ofNullable(builder.features)
				.map(Map::copyOf)
				.orElse(null);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(description, features, name, paths, supportedBinding);
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
		RestEndpointTypeDescription other = (RestEndpointTypeDescription) obj;
		return Objects.equals(description, other.description) && Objects.equals(features, other.features)
				&& Objects.equals(name, other.name) && Objects.equals(paths, other.paths)
				&& Objects.equals(supportedBinding, other.supportedBinding);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private String description;
		private String supportedBinding;
		private Map<String, String> paths;
		private Map<String, String> features;

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

		public Builder withSupportedBinding(String supportedBinding)
		{
			this.supportedBinding = supportedBinding;
			return this;
		}

		public Builder withPaths(Map<String, String> paths)
		{
			this.paths = Optional.ofNullable(paths)
					.map(Map::copyOf)
					.orElse(null);
			return this;
		}

		public Builder withFeatures(Map<String, String> features)
		{
			this.features = Optional.ofNullable(features)
					.map(Map::copyOf)
					.orElse(null);
			return this;
		}

		public RestEndpointTypeDescription build()
		{
			return new RestEndpointTypeDescription(this);
		}
	}

}
