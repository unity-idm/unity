/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBGroupProperty.Builder.class)
class DBGroupProperty
{
	final String key;
	final String value;

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

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private String key;
		private String value;

		private Builder()
		{
		}

		Builder withKey(String key)
		{
			this.key = key;
			return this;
		}

		Builder withValue(String value)
		{
			this.value = value;
			return this;
		}

		DBGroupProperty build()
		{
			return new DBGroupProperty(this);
		}
	}

}
