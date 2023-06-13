/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.translation.out;

import java.time.Duration;
import java.util.Objects;

import pl.edu.icm.unity.base.i18n.I18nString;

public class AuthenticationFinalizationConfiguration
{
	public final I18nString title;
	public final I18nString info;
	public final I18nString redirectCaption;
	public final String redirectURL;
	public final Duration redirectAfterTime;

	private AuthenticationFinalizationConfiguration(Builder builder)
	{
		this.title = builder.title;
		this.info = builder.info;
		this.redirectCaption = builder.redirectCaption;
		this.redirectURL = builder.redirectURL;
		this.redirectAfterTime = builder.redirectAfterTime;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(info, redirectAfterTime, redirectCaption, redirectURL,
				title);
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
		AuthenticationFinalizationConfiguration other = (AuthenticationFinalizationConfiguration) obj;
		return Objects.equals(info, other.info)
				&& Objects.equals(redirectAfterTime, other.redirectAfterTime)
				&& Objects.equals(redirectCaption, other.redirectCaption)
				&& Objects.equals(redirectURL, other.redirectURL) && Objects.equals(title, other.title);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private I18nString title;
		private I18nString info;
		private I18nString redirectCaption;
		private String redirectURL;
		private Duration redirectAfterTime;

		private Builder()
		{
		}

		public Builder withTitle(I18nString title)
		{
			this.title = title;
			return this;
		}

		public Builder withInfo(I18nString info)
		{
			this.info = info;
			return this;
		}

		public Builder withRedirectCaption(I18nString redirectCaption)
		{
			this.redirectCaption = redirectCaption;
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

		public AuthenticationFinalizationConfiguration build()
		{
			return new AuthenticationFinalizationConfiguration(this);
		}
	}

}
