/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.types;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = DBGroupProperty.Builder.class)
public class DBGroupProperty
{
	public final String key;
	public final String value;

	private DBGroupProperty(Builder builder)
	{
		this.key = builder.key;
		this.value = builder.value;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(key, value);
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
		DBGroupProperty other = (DBGroupProperty) obj;
		return Objects.equals(key, other.key) && Objects.equals(value, other.value);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String key;
		private String value;

		private Builder()
		{
		}

		public Builder withKey(String key)
		{
			this.key = key;
			return this;
		}

		public Builder withValue(String value)
		{
			this.value = value;
			return this;
		}

		public DBGroupProperty build()
		{
			return new DBGroupProperty(this);
		}
	}

}
