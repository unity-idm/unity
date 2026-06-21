/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import pl.edu.icm.unity.base.translation.TranslationProfile;

public class OAuthFederationProviderDefaults
{
	public final TranslationProfile translationProfile;
	public final String registrationForm;

	private OAuthFederationProviderDefaults(Builder builder)
	{
		this.translationProfile = builder.translationProfile;
		this.registrationForm = builder.registrationForm;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private TranslationProfile translationProfile;
		private String registrationForm;

		private Builder() {}

		public Builder withTranslationProfile(TranslationProfile translationProfile)
		{
			this.translationProfile = translationProfile;
			return this;
		}

		public Builder withRegistrationForm(String registrationForm)
		{
			this.registrationForm = registrationForm;
			return this;
		}

		public OAuthFederationProviderDefaults build()
		{
			return new OAuthFederationProviderDefaults(this);
		}
	}
}
