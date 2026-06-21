/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Objects;
import java.util.Properties;

public class OAuthClientConfiguration
{
	public final boolean defaultEnableAssociation;
	public final String authenticationCredential;
	public final OAuthFederationConfig federation;
	public final OAuthFederationProviderDefaults federationProviderDefaults;
	public final OAuthProviders providers;
	private final Properties rawProperties;

	private OAuthClientConfiguration(Builder builder)
	{
		this.defaultEnableAssociation = builder.defaultEnableAssociation;
		this.authenticationCredential = builder.authenticationCredential;
		this.federation = builder.federation;
		this.federationProviderDefaults = builder.federationProviderDefaults;
		this.providers = builder.providers;
		this.rawProperties = builder.rawProperties;
	}

	public OAuthProviders providers()
	{
		return providers;
	}

	public Properties getRawProperties()
	{
		return rawProperties;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(authenticationCredential, defaultEnableAssociation, federation,
				federationProviderDefaults, providers);
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
		OAuthClientConfiguration other = (OAuthClientConfiguration) obj;
		return Objects.equals(authenticationCredential, other.authenticationCredential)
				&& defaultEnableAssociation == other.defaultEnableAssociation
				&& Objects.equals(federation, other.federation)
				&& Objects.equals(federationProviderDefaults, other.federationProviderDefaults)
				&& Objects.equals(providers, other.providers);
	}

	public static final class Builder
	{
		private boolean defaultEnableAssociation;
		private String authenticationCredential;
		private OAuthFederationConfig federation;
		private OAuthFederationProviderDefaults federationProviderDefaults;
		private OAuthProviders providers;
		private Properties rawProperties;

		private Builder() {}

		public Builder withDefaultEnableAssociation(boolean defaultEnableAssociation)
		{
			this.defaultEnableAssociation = defaultEnableAssociation;
			return this;
		}

		public Builder withAuthenticationCredential(String authenticationCredential)
		{
			this.authenticationCredential = authenticationCredential;
			return this;
		}

		public Builder withFederation(OAuthFederationConfig federation)
		{
			this.federation = federation;
			return this;
		}

		public Builder withFederationProviderDefaults(OAuthFederationProviderDefaults federationProviderDefaults)
		{
			this.federationProviderDefaults = federationProviderDefaults;
			return this;
		}

		public Builder withProviders(OAuthProviders providers)
		{
			this.providers = providers;
			return this;
		}

		public Builder withRawProperties(Properties rawProperties)
		{
			this.rawProperties = rawProperties;
			return this;
		}

		public OAuthClientConfiguration build()
		{
			return new OAuthClientConfiguration(this);
		}
	}
}
