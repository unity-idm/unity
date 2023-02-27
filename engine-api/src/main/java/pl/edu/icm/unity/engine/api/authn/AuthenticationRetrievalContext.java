/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.authn;

public class AuthenticationRetrievalContext
{
	public final boolean supportOnlySecondFactorReseting;

	private AuthenticationRetrievalContext(Builder builder)
	{
		this.supportOnlySecondFactorReseting = builder.supportOnlySecondFactorReseting;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private boolean supportOnlySecondFactorReseting = false;

		private Builder()
		{
		}

		public Builder withSupportOnlySecondFactorReseting(boolean supportOnlySecondFactorReseting)
		{
			this.supportOnlySecondFactorReseting = supportOnlySecondFactorReseting;
			return this;
		}

		public AuthenticationRetrievalContext build()
		{
			return new AuthenticationRetrievalContext(this);
		}
	}
}
