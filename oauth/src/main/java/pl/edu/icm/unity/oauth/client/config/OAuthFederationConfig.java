/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Objects;
import java.util.Optional;

import com.nimbusds.jose.JWSAlgorithm;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

public class OAuthFederationConfig
{
	public final boolean enabled;
	public final String credential;
	public final String superiorEntityId;
	public final String trustAnchorId;
	public final String jwks;
	public final long metadataValidity;
	public final String truststore;
	public final X509CertChainValidator validator;
	public final ServerHostnameCheckingMode hostnameCheckingMode;
	public final Optional<JWSAlgorithm> jwtSigningAlgorithm;
	public final String organizationName;
	public final String logoUri;

	private OAuthFederationConfig(Builder builder)
	{
		this.enabled = builder.enabled;
		this.credential = builder.credential;
		this.superiorEntityId = builder.superiorEntityId;
		this.trustAnchorId = builder.trustAnchorId;
		this.jwks = builder.jwks;
		this.metadataValidity = builder.metadataValidity;
		this.truststore = builder.truststore;
		this.validator = builder.validator;
		this.hostnameCheckingMode = builder.hostnameCheckingMode;
		this.jwtSigningAlgorithm = builder.jwtSigningAlgorithm;
		this.organizationName = builder.organizationName;
		this.logoUri = builder.logoUri;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	

	@Override
	public int hashCode()
	{
		return Objects.hash(credential, enabled, hostnameCheckingMode, jwks, jwtSigningAlgorithm, metadataValidity,
				superiorEntityId, trustAnchorId, truststore, validator, organizationName, logoUri);
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
		OAuthFederationConfig other = (OAuthFederationConfig) obj;
		return Objects.equals(credential, other.credential) && enabled == other.enabled
				&& hostnameCheckingMode == other.hostnameCheckingMode && Objects.equals(jwks, other.jwks)
				&& Objects.equals(jwtSigningAlgorithm, other.jwtSigningAlgorithm)
				&& metadataValidity == other.metadataValidity
				&& Objects.equals(superiorEntityId, other.superiorEntityId)
				&& Objects.equals(trustAnchorId, other.trustAnchorId) && Objects.equals(truststore, other.truststore)
				&& Objects.equals(validator, other.validator)
				&& Objects.equals(organizationName, other.organizationName)
				&& Objects.equals(logoUri, other.logoUri);
	}

	public static final class Builder
	{
		private boolean enabled;
		private String credential;
		private String superiorEntityId;
		private String trustAnchorId;
		private String jwks;
		private long metadataValidity;
		private String truststore;
		private X509CertChainValidator validator;
		private ServerHostnameCheckingMode hostnameCheckingMode;
		private Optional<JWSAlgorithm> jwtSigningAlgorithm = Optional.empty();
		private String organizationName;
		private String logoUri;

		private Builder() {}

		public Builder withEnabled(boolean enabled)
		{
			this.enabled = enabled;
			return this;
		}

		public Builder withCredential(String credential)
		{
			this.credential = credential;
			return this;
		}

		public Builder withSuperiorEntityId(String superiorEntityId)
		{
			this.superiorEntityId = superiorEntityId;
			return this;
		}

		public Builder withTrustAnchorId(String trustAnchorId)
		{
			this.trustAnchorId = trustAnchorId;
			return this;
		}

		public Builder withJwks(String jwks)
		{
			this.jwks = jwks;
			return this;
		}

		public Builder withMetadataValidity(long metadataValidity)
		{
			this.metadataValidity = metadataValidity;
			return this;
		}

		public Builder withTruststore(String truststore)
		{
			this.truststore = truststore;
			return this;
		}

		public Builder withValidator(X509CertChainValidator validator)
		{
			this.validator = validator;
			return this;
		}

		public Builder withHostnameCheckingMode(ServerHostnameCheckingMode hostnameCheckingMode)
		{
			this.hostnameCheckingMode = hostnameCheckingMode;
			return this;
		}

		public Builder withJwtSigningAlgorithm(Optional<JWSAlgorithm> jwtSigningAlgorithm)
		{
			this.jwtSigningAlgorithm = jwtSigningAlgorithm;
			return this;
		}

		public Builder withOrganizationName(String organizationName)
		{
			this.organizationName = organizationName;
			return this;
		}

		public Builder withLogoUri(String logoUri)
		{
			this.logoUri = logoUri;
			return this;
		}

		public OAuthFederationConfig build()
		{
			return new OAuthFederationConfig(this);
		}
	}
}
