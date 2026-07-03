/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.List;

import pl.edu.icm.unity.base.translation.TranslationProfile;

public record OAuthFederationProviderDefaults(
		TranslationProfile translationProfile,
		String registrationForm,
		RequestACRsMode requestACRsMode,
		List<String> requestedACRs,
		boolean requestedACRsAreEssential)
{
	public OAuthFederationProviderDefaults
	{
		requestACRsMode = requestACRsMode != null ? requestACRsMode : RequestACRsMode.NONE;
		requestedACRs = requestedACRs != null ? List.copyOf(requestedACRs) : List.of();
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private TranslationProfile translationProfile;
		private String registrationForm;
		private RequestACRsMode requestACRsMode;
		private List<String> requestedACRs;
		private boolean requestedACRsAreEssential;

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

		public Builder withRequestACRsMode(RequestACRsMode requestACRsMode)
		{
			this.requestACRsMode = requestACRsMode;
			return this;
		}

		public Builder withRequestedACRs(List<String> requestedACRs)
		{
			this.requestedACRs = requestedACRs;
			return this;
		}

		public Builder withRequestedACRsAreEssential(boolean requestedACRsAreEssential)
		{
			this.requestedACRsAreEssential = requestedACRsAreEssential;
			return this;
		}

		public OAuthFederationProviderDefaults build()
		{
			return new OAuthFederationProviderDefaults(translationProfile, registrationForm, requestACRsMode,
					requestedACRs, requestedACRsAreEssential);
		}
	}
}
