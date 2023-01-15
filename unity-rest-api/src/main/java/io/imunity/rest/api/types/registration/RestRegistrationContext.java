/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestRegistrationContext.Builder.class)
public class RestRegistrationContext
{
	public final boolean isOnIdpEndpoint;
	public final String triggeringMode;

	private RestRegistrationContext(Builder builder)
	{
		this.isOnIdpEndpoint = builder.isOnIdpEndpoint;
		this.triggeringMode = builder.triggeringMode;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(isOnIdpEndpoint, triggeringMode);
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
		RestRegistrationContext other = (RestRegistrationContext) obj;
		return isOnIdpEndpoint == other.isOnIdpEndpoint && Objects.equals(triggeringMode, other.triggeringMode);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private boolean isOnIdpEndpoint;
		private String triggeringMode;

		private Builder()
		{
		}

		public Builder withIsOnIdpEndpoint(boolean isOnIdpEndpoint)
		{
			this.isOnIdpEndpoint = isOnIdpEndpoint;
			return this;
		}

		public Builder withTriggeringMode(String triggeringMode)
		{
			this.triggeringMode = triggeringMode;
			return this;
		}

		public RestRegistrationContext build()
		{
			return new RestRegistrationContext(this);
		}
	}

}
