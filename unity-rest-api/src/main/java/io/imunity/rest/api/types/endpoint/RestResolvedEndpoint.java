/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.endpoint;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.rest.api.types.authn.RestAuthenticationRealm;

@JsonDeserialize(builder = RestResolvedEndpoint.Builder.class)
public class RestResolvedEndpoint
{
	public final RestEndpoint endpoint;
	public final RestAuthenticationRealm realm;
	public final RestEndpointTypeDescription type;

	private RestResolvedEndpoint(Builder builder)
	{
		this.endpoint = builder.endpoint;
		this.realm = builder.realm;
		this.type = builder.type;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(endpoint, realm, type);
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
		RestResolvedEndpoint other = (RestResolvedEndpoint) obj;
		return Objects.equals(endpoint, other.endpoint)
				&& Objects.equals(realm, other.realm)
				&& Objects.equals(type, other.type);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private RestEndpoint endpoint;
		private RestAuthenticationRealm realm;
		private RestEndpointTypeDescription type;

		private Builder()
		{
		}

		public Builder withEndpoint(RestEndpoint endpoint)
		{
			this.endpoint = endpoint;
			return this;
		}

		public Builder withRealm(RestAuthenticationRealm realm)
		{
			this.realm = realm;
			return this;
		}

		public Builder withType(RestEndpointTypeDescription type)
		{
			this.type = type;
			return this;
		}

		public RestResolvedEndpoint build()
		{
			return new RestResolvedEndpoint(this);
		}
	}

}
