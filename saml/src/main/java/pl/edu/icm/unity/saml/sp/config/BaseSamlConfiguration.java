/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.config;

import pl.edu.icm.unity.saml.sp.SAMLSPProperties.MetadataSignatureValidation;
import pl.edu.icm.unity.types.translation.TranslationProfile;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base configuration for both authenticators and IdPs
 */
public abstract class BaseSamlConfiguration
{
	public final Map<String, RemoteMetadataSource> trustedMetadataSourcesByUrl;
	public final boolean publishMetadata;
	public final String metadataURLPath;
	public final String ourMetadataFilePath;

	public BaseSamlConfiguration(List<RemoteMetadataSource> trustedMetadataSources, boolean publishMetadata,
			String metadataURLPath, String ourMetadataFilePath)
	{
		this.trustedMetadataSourcesByUrl = trustedMetadataSources.stream()
				.collect(Collectors.toUnmodifiableMap(src -> src.url, src -> src));
		this.publishMetadata = publishMetadata;
		this.metadataURLPath = metadataURLPath;
		this.ourMetadataFilePath = ourMetadataFilePath;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(metadataURLPath, ourMetadataFilePath, publishMetadata, trustedMetadataSourcesByUrl);
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
		BaseSamlConfiguration other = (BaseSamlConfiguration) obj;
		return Objects.equals(metadataURLPath, other.metadataURLPath)
				&& Objects.equals(ourMetadataFilePath, other.ourMetadataFilePath)
				&& publishMetadata == other.publishMetadata
				&& Objects.equals(trustedMetadataSourcesByUrl, other.trustedMetadataSourcesByUrl);
	}


	public static class RemoteMetadataSource
	{
		public final String url;
		public final Duration refreshInterval;
		public final String httpsTruststore;
		public final MetadataSignatureValidation signatureValidation;
		public final String issuerCertificate;
		public final String registrationForm;
		public final TranslationProfile translationProfile;
		public final Set<String> excludedIdps;
		
		private RemoteMetadataSource(Builder builder)
		{
			checkNotNull(builder.translationProfile);
			checkNotNull(builder.url);
			checkNotNull(builder.refreshInterval);
			checkNotNull(builder.excludedIdps);
			this.url = builder.url;
			this.refreshInterval = builder.refreshInterval;
			this.httpsTruststore = builder.httpsTruststore;
			this.signatureValidation = builder.signatureValidation;
			this.issuerCertificate = builder.issuerCertificate;
			this.registrationForm = builder.registrationForm;
			this.translationProfile = builder.translationProfile;
			this.excludedIdps = Set.copyOf(builder.excludedIdps);
		}
		
		@Override
		public int hashCode()
		{
			return Objects.hash(httpsTruststore, issuerCertificate, refreshInterval,
					registrationForm, signatureValidation, translationProfile, url, excludedIdps);
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
			RemoteMetadataSource other = (RemoteMetadataSource) obj;
			return Objects.equals(httpsTruststore, other.httpsTruststore)
					&& Objects.equals(issuerCertificate, other.issuerCertificate)
					&& Objects.equals(refreshInterval, other.refreshInterval)
					&& Objects.equals(registrationForm, other.registrationForm)
					&& signatureValidation == other.signatureValidation
					&& Objects.equals(translationProfile, other.translationProfile)
					&& Objects.equals(url, other.url)
					&& Objects.equals(excludedIdps, other.excludedIdps);
		}

		@Override
		public String toString()
		{
			return "RemoteMetadataSource{" +
					"url='" + url + '\'' +
					", refreshInterval=" + refreshInterval +
					", httpsTruststore='" + httpsTruststore + '\'' +
					", signatureValidation=" + signatureValidation +
					", issuerCertificate='" + issuerCertificate + '\'' +
					", registrationForm='" + registrationForm + '\'' +
					", translationProfile=" + translationProfile +
					", excludedIdps=" + excludedIdps +
					'}';
		}

		public static Builder builder()
		{
			return new Builder();
		}
		
		public static final class Builder
		{
			private String url;
			private Duration refreshInterval;
			private String httpsTruststore;
			private MetadataSignatureValidation signatureValidation;
			private String issuerCertificate;
			private String registrationForm;
			private TranslationProfile translationProfile;
			private Set<String> excludedIdps = Collections.emptySet();
			
			private Builder()
			{
			}

			public Builder withUrl(String url)
			{
				this.url = url;
				return this;
			}

			public Builder withRefreshInterval(Duration refreshInterval)
			{
				this.refreshInterval = refreshInterval;
				return this;
			}

			public Builder withHttpsTruststore(String httpsTruststore)
			{
				this.httpsTruststore = httpsTruststore;
				return this;
			}

			public Builder withSignatureValidation(MetadataSignatureValidation signatureValidation)
			{
				this.signatureValidation = signatureValidation;
				return this;
			}

			public Builder withIssuerCertificate(String issuerCertificate)
			{
				this.issuerCertificate = issuerCertificate;
				return this;
			}

			public Builder withRegistrationForm(String registrationForm)
			{
				this.registrationForm = registrationForm;
				return this;
			}
			
			public Builder withTranslationProfile(TranslationProfile translationProfile)
			{
				this.translationProfile = translationProfile;
				return this;
			}
			
			public Builder withExcludedIdps(Set<String> excludedIdps)
			{
				this.excludedIdps = excludedIdps;
				return this;
			}
			
			public RemoteMetadataSource build()
			{
				return new RemoteMetadataSource(this);
			}
		}
	}
}
