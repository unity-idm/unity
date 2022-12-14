/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.authn;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestAuthenticationRealm.Builder.class)
public class RestAuthenticationRealm
{
	public final String name;
	public final String description;
	public final int blockAfterUnsuccessfulLogins;
	public final int blockFor;
	public final int allowForRememberMeDays;
	public final String rememberMePolicy;
	public final int maxInactivity;

	private RestAuthenticationRealm(Builder builder)
	{
		this.name = builder.name;
		this.description = builder.description;
		this.blockAfterUnsuccessfulLogins = builder.blockAfterUnsuccessfulLogins;
		this.blockFor = builder.blockFor;
		this.allowForRememberMeDays = builder.allowForRememberMeDays;
		this.rememberMePolicy = builder.rememberMePolicy;
		this.maxInactivity = builder.maxInactivity;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(allowForRememberMeDays, blockAfterUnsuccessfulLogins, blockFor, description, maxInactivity,
				name, rememberMePolicy);
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
		RestAuthenticationRealm other = (RestAuthenticationRealm) obj;
		return allowForRememberMeDays == other.allowForRememberMeDays
				&& blockAfterUnsuccessfulLogins == other.blockAfterUnsuccessfulLogins && blockFor == other.blockFor
				&& Objects.equals(description, other.description) && maxInactivity == other.maxInactivity
				&& Objects.equals(name, other.name) && Objects.equals(rememberMePolicy, other.rememberMePolicy);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private String description;
		private int blockAfterUnsuccessfulLogins;
		private int blockFor;
		private int allowForRememberMeDays;
		private String rememberMePolicy;
		private int maxInactivity;

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

		public Builder withBlockAfterUnsuccessfulLogins(int blockAfterUnsuccessfulLogins)
		{
			this.blockAfterUnsuccessfulLogins = blockAfterUnsuccessfulLogins;
			return this;
		}

		public Builder withBlockFor(int blockFor)
		{
			this.blockFor = blockFor;
			return this;
		}

		public Builder withAllowForRememberMeDays(int allowForRememberMeDays)
		{
			this.allowForRememberMeDays = allowForRememberMeDays;
			return this;
		}

		public Builder withRememberMePolicy(String rememberMePolicy)
		{
			this.rememberMePolicy = rememberMePolicy;
			return this;
		}

		public Builder withMaxInactivity(int maxInactivity)
		{
			this.maxInactivity = maxInactivity;
			return this;
		}

		public RestAuthenticationRealm build()
		{
			return new RestAuthenticationRealm(this);
		}
	}

}
