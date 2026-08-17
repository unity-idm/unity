/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Optional;

import com.nimbusds.jose.JWSAlgorithm;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

public record OAuthFederationConfig(
		boolean enabled,
		String credential,
		String superiorEntityId,
		String trustAnchorId,
		String jwks,
		long metadataValidity,
		String truststore,
		X509CertChainValidator validator,
		ServerHostnameCheckingMode hostnameCheckingMode,
		Optional<JWSAlgorithm> jwtSigningAlgorithm,
		String organizationName,
		String logoUri)
{
	public static Builder builder()
	{
		return new Builder();
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
			return new OAuthFederationConfig(enabled, credential, superiorEntityId, trustAnchorId, jwks,
					metadataValidity, truststore, validator, hostnameCheckingMode, jwtSigningAlgorithm,
					organizationName, logoUri);
		}
	}
}
