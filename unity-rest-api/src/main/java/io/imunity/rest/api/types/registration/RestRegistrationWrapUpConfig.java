/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.time.Duration;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.rest.api.types.basic.RestI18nString;

@JsonDeserialize(builder = RestRegistrationWrapUpConfig.Builder.class)
public class RestRegistrationWrapUpConfig
{
	public final String state;
	public final RestI18nString title;
	public final RestI18nString info;
	public final RestI18nString redirectCaption;
	public final boolean automatic;
	public final String redirectURL;
	public final Duration redirectAfterTime;

	private RestRegistrationWrapUpConfig(Builder builder)
	{
		this.state = builder.state;
		this.title = builder.title;
		this.info = builder.info;
		this.redirectCaption = builder.redirectCaption;
		this.automatic = builder.automatic;
		this.redirectURL = builder.redirectURL;
		this.redirectAfterTime = builder.redirectAfterTime;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(automatic, info, redirectAfterTime, redirectCaption, redirectURL, state, title);
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
		RestRegistrationWrapUpConfig other = (RestRegistrationWrapUpConfig) obj;
		return automatic == other.automatic && Objects.equals(info, other.info)
				&& Objects.equals(redirectAfterTime, other.redirectAfterTime)
				&& Objects.equals(redirectCaption, other.redirectCaption)
				&& Objects.equals(redirectURL, other.redirectURL) && Objects.equals(state, other.state)
				&& Objects.equals(title, other.title);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String state;
		private RestI18nString title;
		private RestI18nString info;
		private RestI18nString redirectCaption;
		private boolean automatic;
		private String redirectURL;
		private Duration redirectAfterTime;

		private Builder()
		{
		}

		public Builder withState(String state)
		{
			this.state = state;
			return this;
		}

		public Builder withTitle(RestI18nString title)
		{
			this.title = title;
			return this;
		}

		public Builder withInfo(RestI18nString info)
		{
			this.info = info;
			return this;
		}

		public Builder withRedirectCaption(RestI18nString redirectCaption)
		{
			this.redirectCaption = redirectCaption;
			return this;
		}

		public Builder withAutomatic(boolean automatic)
		{
			this.automatic = automatic;
			return this;
		}

		public Builder withRedirectURL(String redirectURL)
		{
			this.redirectURL = redirectURL;
			return this;
		}

		public Builder withRedirectAfterTime(Duration redirectAfterTime)
		{
			this.redirectAfterTime = redirectAfterTime;
			return this;
		}

		public RestRegistrationWrapUpConfig build()
		{
			return new RestRegistrationWrapUpConfig(this);
		}
	}

}
