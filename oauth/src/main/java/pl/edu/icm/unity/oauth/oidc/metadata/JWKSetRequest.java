/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.oidc.metadata;

import java.util.Objects;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

public class JWKSetRequest
{
	public final String url;
	public final ServerHostnameCheckingMode hostnameChecking;
	public final String validatorName;
	public final X509CertChainValidator validator;

	private JWKSetRequest(Builder builder)
	{
		this.url = builder.url;
		this.hostnameChecking = builder.hostnameChecking;
		this.validatorName = builder.validatorName;
		this.validator = builder.validator;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(hostnameChecking, url, validator, validatorName);
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
		JWKSetRequest other = (JWKSetRequest) obj;
		return hostnameChecking == other.hostnameChecking && Objects.equals(url, other.url)
				&& Objects.equals(validator, other.validator) && Objects.equals(validatorName, other.validatorName);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String url;
		private ServerHostnameCheckingMode hostnameChecking;
		private String validatorName;
		private X509CertChainValidator validator;

		private Builder()
		{
		}

		public Builder withUrl(String url)
		{
			this.url = url;
			return this;
		}

		public Builder withHostnameChecking(ServerHostnameCheckingMode hostnameChecking)
		{
			this.hostnameChecking = hostnameChecking;
			return this;
		}

		public Builder withValidatorName(String validatorName)
		{
			this.validatorName = validatorName;
			return this;
		}

		public Builder withValidator(X509CertChainValidator validator)
		{
			this.validator = validator;
			return this;
		}

		public JWKSetRequest build()
		{
			return new JWKSetRequest(this);
		}
	}

}
