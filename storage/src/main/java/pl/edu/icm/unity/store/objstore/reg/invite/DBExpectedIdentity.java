/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBExpectedIdentity.Builder.class)
public class DBExpectedIdentity
{
	public final String identity;
	public final String expectation;

	private DBExpectedIdentity(Builder builder)
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
		DBExpectedIdentity other = (DBExpectedIdentity) obj;
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

		public DBExpectedIdentity build()
		{
			return new DBExpectedIdentity(this);
		}
	}

}
