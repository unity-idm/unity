/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Objects;
import java.util.Properties;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.base.translation.TranslationProfile;

public class OAuthClientConfiguration
{
	public final boolean defaultEnableAssociation;
	public final boolean federationMembershipEnabled;
	public final String federationCredential;
	public final String authenticationCredential;
	public final String federationSuperiorEntityId;
	public final String federationTrustAnchorId;
	public final String federationJwks;
	public final long federationMetadataValidity;
	public final OAuthProviders providers;
	public final String federationTruststore;
	public final X509CertChainValidator federationValidator;
	public final ServerHostnameCheckingMode federationHostnameCheckingMode;
	public final TranslationProfile federationTranslationProfile;
	public final String federationRegistrationForm;
	private final Properties rawProperties;

	private OAuthClientConfiguration(Builder builder)
	{
		this.defaultEnableAssociation = builder.defaultEnableAssociation;
		this.federationMembershipEnabled = builder.federationMembershipEnabled;
		this.federationCredential = builder.federationCredential;
		this.authenticationCredential = builder.authenticationCredential;
		this.federationSuperiorEntityId = builder.federationSuperiorEntityId;
		this.federationTrustAnchorId = builder.federationTrustAnchorId;
		this.federationJwks = builder.federationJwks;
		this.federationMetadataValidity = builder.federationMetadataValidity;
		this.providers = builder.providers;
		this.federationTruststore = builder.federationTruststore;
		this.federationValidator = builder.federationValidator;
		this.federationHostnameCheckingMode = builder.federationHostnameCheckingMode;
		this.federationTranslationProfile = builder.federationTranslationProfile;
		this.federationRegistrationForm = builder.federationRegistrationForm;
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
		return Objects.hash(authenticationCredential, defaultEnableAssociation, federationCredential,
				federationJwks, federationMembershipEnabled, federationMetadataValidity,
				federationSuperiorEntityId, federationTrustAnchorId, providers);
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
				&& Objects.equals(federationCredential, other.federationCredential)
				&& Objects.equals(federationJwks, other.federationJwks)
				&& federationMembershipEnabled == other.federationMembershipEnabled
				&& federationMetadataValidity == other.federationMetadataValidity
				&& Objects.equals(federationSuperiorEntityId, other.federationSuperiorEntityId)
				&& Objects.equals(federationTrustAnchorId, other.federationTrustAnchorId)
				&& Objects.equals(providers, other.providers);
	}

	public static final class Builder
	{
		private boolean defaultEnableAssociation;
		private boolean federationMembershipEnabled;
		private String federationCredential;
		private String authenticationCredential;
		private String federationSuperiorEntityId;
		private String federationTrustAnchorId;
		private String federationJwks;
		private long federationMetadataValidity;
		private OAuthProviders providers;
		private String federationTruststore;
		private X509CertChainValidator federationValidator;
		private ServerHostnameCheckingMode federationHostnameCheckingMode;
		private TranslationProfile federationTranslationProfile;
		private String federationRegistrationForm;
		private Properties rawProperties;

		private Builder() {}

		public Builder withDefaultEnableAssociation(boolean defaultEnableAssociation)
		{
			this.defaultEnableAssociation = defaultEnableAssociation;
			return this;
		}

		public Builder withFederationMembershipEnabled(boolean federationMembershipEnabled)
		{
			this.federationMembershipEnabled = federationMembershipEnabled;
			return this;
		}

		public Builder withFederationCredential(String federationCredential)
		{
			this.federationCredential = federationCredential;
			return this;
		}

		public Builder withAuthenticationCredential(String authenticationCredential)
		{
			this.authenticationCredential = authenticationCredential;
			return this;
		}

		public Builder withFederationSuperiorEntityId(String federationSuperiorEntityId)
		{
			this.federationSuperiorEntityId = federationSuperiorEntityId;
			return this;
		}

		public Builder withFederationTrustAnchorId(String federationTrustAnchorId)
		{
			this.federationTrustAnchorId = federationTrustAnchorId;
			return this;
		}

		public Builder withFederationJwks(String federationJwks)
		{
			this.federationJwks = federationJwks;
			return this;
		}

		public Builder withFederationMetadataValidity(long federationMetadataValidity)
		{
			this.federationMetadataValidity = federationMetadataValidity;
			return this;
		}

		public Builder withProviders(OAuthProviders providers)
		{
			this.providers = providers;
			return this;
		}

		public Builder withFederationTruststore(String federationTruststore)
		{
			this.federationTruststore = federationTruststore;
			return this;
		}

		public Builder withFederationValidator(X509CertChainValidator federationValidator)
		{
			this.federationValidator = federationValidator;
			return this;
		}

		public Builder withFederationHostnameCheckingMode(ServerHostnameCheckingMode federationHostnameCheckingMode)
		{
			this.federationHostnameCheckingMode = federationHostnameCheckingMode;
			return this;
		}

		public Builder withFederationTranslationProfile(TranslationProfile federationTranslationProfile)
		{
			this.federationTranslationProfile = federationTranslationProfile;
			return this;
		}

		public Builder withFederationRegistrationForm(String federationRegistrationForm)
		{
			this.federationRegistrationForm = federationRegistrationForm;
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
