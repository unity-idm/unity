/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestToken.Builder.class)
public class RestToken
{
	public final String type;
	public final String value;
	public final Long owner;
	public final Date created;
	public final Date expires;
	public final byte[] contents;

	private RestToken(Builder builder)
	{
		this.type = builder.type;
		this.value = builder.value;
		this.owner = builder.owner;
		this.created = builder.created;
		this.expires = builder.expires;
		this.contents = builder.contents;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(contents);
		result = prime * result + Objects.hash(created, expires, owner, type, value);
		return result;
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
		RestToken other = (RestToken) obj;
		return Arrays.equals(contents, other.contents) && Objects.equals(created, other.created)
				&& Objects.equals(expires, other.expires) && Objects.equals(owner, other.owner)
				&& Objects.equals(type, other.type) && Objects.equals(value, other.value);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String type;
		private String value;
		private Long owner;
		private Date created;
		private Date expires;
		private byte[] contents;

		private Builder()
		{
		}

		public Builder withType(String type)
		{
			this.type = type;
			return this;
		}

		public Builder withValue(String value)
		{
			this.value = value;
			return this;
		}

		public Builder withOwner(Long owner)
		{
			this.owner = owner;
			return this;
		}

		public Builder withCreated(Date created)
		{
			this.created = created;
			return this;
		}

		public Builder withExpires(Date expires)
		{
			this.expires = expires;
			return this;
		}

		public Builder withContents(byte[] contents)
		{
			this.contents = contents;
			return this;
		}

		public RestToken build()
		{
			return new RestToken(this);
		}
	}	
}
