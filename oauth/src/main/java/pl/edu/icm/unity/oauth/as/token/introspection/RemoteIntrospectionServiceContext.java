/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.introspection;

import java.net.URL;

import com.nimbusds.jose.JWSVerifier;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

class RemoteIntrospectionServiceContext
{
	final String clientId;
	final String clientSecret;
	final String issuer;
	final JWSVerifier verifier;
	final URL url;
	final ServerHostnameCheckingMode hostnameCheckingMode;
	final X509CertChainValidator validator;

	private RemoteIntrospectionServiceContext(RemoteIntrospectionServiceContext.Builder builder)
	{
		this.clientId = builder.clientId;
		this.clientSecret = builder.clientSecret;
		this.issuer = builder.issuer;
		this.verifier = builder.verifier;
		this.url = builder.url;
		this.hostnameCheckingMode = builder.hostnameCheckingMode;
		this.validator = builder.validator;
	}

	static RemoteIntrospectionServiceContext.Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private String clientId;
		private String clientSecret;
		private String issuer;
		private JWSVerifier verifier;
		private URL url;
		private ServerHostnameCheckingMode hostnameCheckingMode;
		private X509CertChainValidator validator;

		private Builder()
		{
		}

		RemoteIntrospectionServiceContext.Builder withClientId(String clientId)
		{
			this.clientId = clientId;
			return this;
		}

		RemoteIntrospectionServiceContext.Builder withClientSecret(String clientSecret)
		{
			this.clientSecret = clientSecret;
			return this;
		}

		RemoteIntrospectionServiceContext.Builder withIssuer(String issuer)
		{
			this.issuer = issuer;
			return this;
		}

		RemoteIntrospectionServiceContext.Builder withVerifier(JWSVerifier verifier)
		{
			this.verifier = verifier;
			return this;
		}

		RemoteIntrospectionServiceContext.Builder withUrl(URL url)
		{
			this.url = url;
			return this;
		}

		RemoteIntrospectionServiceContext.Builder withHostnameCheckingMode(ServerHostnameCheckingMode hostnameCheckingMode)
		{
			this.hostnameCheckingMode = hostnameCheckingMode;
			return this;
		}

		RemoteIntrospectionServiceContext.Builder withValidator(X509CertChainValidator validator)
		{
			this.validator = validator;
			return this;
		}

		RemoteIntrospectionServiceContext build()
		{
			return new RemoteIntrospectionServiceContext(this);
		}
	}
}