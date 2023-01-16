/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration.invite;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestExpectedIdentity.Builder.class)
public class RestExpectedIdentity
{
	public final String identity;
	public final String expectation;

	private RestExpectedIdentity(Builder builder)
	{
		this.identity = builder.identity;
		this.expectation = builder.expectation;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(expectation, identity);
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
		RestExpectedIdentity other = (RestExpectedIdentity) obj;
		return Objects.equals(expectation, other.expectation) && Objects.equals(identity, other.identity);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String identity;
		private String expectation;

		private Builder()
		{
		}

		public Builder withIdentity(String identity)
		{
			this.identity = identity;
			return this;
		}

		public Builder withExpectation(String expectation)
		{
			this.expectation = expectation;
			return this;
		}

		public RestExpectedIdentity build()
		{
			return new RestExpectedIdentity(this);
		}
	}

}
