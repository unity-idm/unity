/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.trust.SamlTrustChecker;

public class SAMLSPConfiguration extends BaseSamlConfiguration
{
	public final String requesterSamlId;
	public final String sloPath;
	public final String sloRealm;
	public final X509Credential requesterCredential;
	public final String requesterCredentialName;
	public final SamlTrustChecker trustChecker;
	public final boolean signRequestByDefault;
	public final List<String> acceptedNameFormats;
	public final boolean signPublishedMetadata;
	public final Map<String, String> effectiveMappings;
	public final TrustedIdPs individualTrustedIdPs;

	private SAMLSPConfiguration(Builder builder)
	{
		super(builder.trustedMetadataSources, builder.publishMetadata, builder.metadataURLPath,
				builder.ourMetadataFilePath);
		checkNotNull(builder.requesterSamlId);
		checkNotNull(builder.trustChecker);
		checkNotNull(builder.acceptedNameFormats);
		checkNotNull(builder.effectiveMappings);
		checkNotNull(builder.individualTrustedIdPs);

		this.requesterSamlId = builder.requesterSamlId;
		this.sloPath = builder.sloPath;
		this.sloRealm = builder.sloRealm;
		this.requesterCredential = builder.requesterCredential;
		this.requesterCredentialName = builder.requesterCredentialName;
		this.trustChecker = builder.trustChecker;
		this.signRequestByDefault = builder.signRequestByDefault;
		this.acceptedNameFormats = List.copyOf(builder.acceptedNameFormats);
		this.signPublishedMetadata = builder.signPublishedMetadata;
		this.effectiveMappings = Map.copyOf(builder.effectiveMappings);
		this.individualTrustedIdPs = builder.individualTrustedIdPs;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private List<RemoteMetadataSource> trustedMetadataSources = Collections.emptyList();
		private boolean publishMetadata;
		private String metadataURLPath;
		private String ourMetadataFilePath;
		private String requesterSamlId;
		private String sloPath;
		private String sloRealm;
		private X509Credential requesterCredential;
		private String requesterCredentialName;
		private SamlTrustChecker trustChecker;
		private boolean signRequestByDefault;
		private List<String> acceptedNameFormats = Collections.emptyList();
		private boolean signPublishedMetadata;
		private Map<String, String> effectiveMappings = Collections.emptyMap();
		private TrustedIdPs individualTrustedIdPs;

		private Builder()
		{
		}

		public Builder withTrustedMetadataSources(List<RemoteMetadataSource> trustedMetadataSources)
		{
			this.trustedMetadataSources = trustedMetadataSources;
			return this;
		}

		public Builder withPublishMetadata(boolean publishMetadata)
		{
			this.publishMetadata = publishMetadata;
			return this;
		}

		public Builder withMetadataURLPath(String metadataURLPath)
		{
			this.metadataURLPath = metadataURLPath;
			return this;
		}

		public Builder withOurMetadataFilePath(String ourMetadataFilePath)
		{
			this.ourMetadataFilePath = ourMetadataFilePath;
			return this;
		}

		public Builder withRequesterSamlId(String requesterSamlId)
		{
			this.requesterSamlId = requesterSamlId;
			return this;
		}

		public Builder withSloPath(String sloPath)
		{
			this.sloPath = sloPath;
			return this;
		}

		public Builder withSloRealm(String sloRealm)
		{
			this.sloRealm = sloRealm;
			return this;
		}

		public Builder withRequesterCredential(X509Credential requesterCredential)
		{
			this.requesterCredential = requesterCredential;
			return this;
		}

		public Builder withRequesterCredentialName(String requesterCredentialName)
		{
			this.requesterCredentialName = requesterCredentialName;
			return this;
		}

		public Builder withTrustChecker(SamlTrustChecker trustChecker)
		{
			this.trustChecker = trustChecker;
			return this;
		}

		public Builder withSignRequestByDefault(boolean signRequestByDefault)
		{
			this.signRequestByDefault = signRequestByDefault;
			return this;
		}

		public Builder withAcceptedNameFormats(List<String> acceptedNameFormats)
		{
			this.acceptedNameFormats = acceptedNameFormats;
			return this;
		}

		public Builder withSignPublishedMetadata(boolean signPublishedMetadata)
		{
			this.signPublishedMetadata = signPublishedMetadata;
			return this;
		}

		public Builder withEffectiveMappings(Map<String, String> effectiveMappings)
		{
			this.effectiveMappings = effectiveMappings;
			return this;
		}

		public Builder withIndividualTrustedIdPs(TrustedIdPs individualTrustedIdPs)
		{
			this.individualTrustedIdPs = individualTrustedIdPs;
			return this;
		}

		public SAMLSPConfiguration build()
		{
			return new SAMLSPConfiguration(this);
		}
	}

	
	
}
