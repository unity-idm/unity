/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.introspection;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

class TrustedUpstreamConfiguration
{
	final String clientId;
	final String clientSecret;
	final String metadataURL;
	final String issuerURI;
	final String introspectionEndpointURL;
	final String certificate;
	final ServerHostnameCheckingMode clientHostnameChecking;
	final String clientTrustStore;

	private TrustedUpstreamConfiguration(Builder builder)
	{
		this.clientId = builder.clientId;
		this.clientSecret = builder.clientSecret;
		this.metadataURL = builder.metadataURL;
		this.issuerURI = builder.issuerURI;
		this.introspectionEndpointURL = builder.introspectionEndpointURL;
		this.certificate = builder.certificate;
		this.clientHostnameChecking = builder.clientHostnameChecking;
		this.clientTrustStore = builder.clientTrustStore;
	}

	boolean isMetadata()
	{
		return !(metadataURL == null);
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private String clientId;
		private String clientSecret;
		private String metadataURL;
		private String issuerURI;
		private String introspectionEndpointURL;
		private String certificate;
		private ServerHostnameCheckingMode clientHostnameChecking;
		private String clientTrustStore;

		private Builder()
		{
		}

		public Builder withClientId(String clientId)
		{
			this.clientId = clientId;
			return this;
		}

		public Builder withClientSecret(String clientSecret)
		{
			this.clientSecret = clientSecret;
			return this;
		}

		public Builder withMetadataURL(String metadataURL)
		{
			this.metadataURL = metadataURL;
			return this;
		}

		public Builder withIssuerURI(String issuerURI)
		{
			this.issuerURI = issuerURI;
			return this;
		}

		public Builder withIntrospectionEndpointURL(String introspectionEndpointURL)
		{
			this.introspectionEndpointURL = introspectionEndpointURL;
			return this;
		}

		public Builder withCertificate(String certificate)
		{
			this.certificate = certificate;
			return this;
		}

		public Builder withClientHostnameChecking(ServerHostnameCheckingMode clientHostnameChecking)
		{
			this.clientHostnameChecking = clientHostnameChecking;
			return this;
		}

		public Builder withClientTrustStore(String clientTrustStore)
		{
			this.clientTrustStore = clientTrustStore;
			return this;
		}

		public TrustedUpstreamConfiguration build()
		{
			return new TrustedUpstreamConfiguration(this);
		}
	}

}
