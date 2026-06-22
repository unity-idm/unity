/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.List;

import pl.edu.icm.unity.base.translation.TranslationProfile;

public class OAuthFederationProviderDefaults
{
	public final TranslationProfile translationProfile;
	public final String registrationForm;
	public final RequestACRsMode requestACRsMode;
	public final List<String> requestedACRs;
	public final boolean requestedACRsAreEssential;

	private OAuthFederationProviderDefaults(Builder builder)
	{
		this.translationProfile = builder.translationProfile;
		this.registrationForm = builder.registrationForm;
		this.requestACRsMode = builder.requestACRsMode != null ? builder.requestACRsMode : RequestACRsMode.NONE;
		this.requestedACRs = builder.requestedACRs != null ? List.copyOf(builder.requestedACRs) : List.of();
		this.requestedACRsAreEssential = builder.requestedACRsAreEssential;
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
			return new OAuthFederationProviderDefaults(this);
		}
	}
}
