/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.messages;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBMessage.Builder.class)
class DBMessage
{
	public final String name;
	public final String locale;
	public final String value;

	private DBMessage(Builder builder)
	{
		this.name = builder.name;
		this.locale = builder.locale;
		this.value = builder.value;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(locale, name, value);
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
		DBMessage other = (DBMessage) obj;
		return Objects.equals(locale, other.locale) && Objects.equals(name, other.name)
				&& Objects.equals(value, other.value);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private String locale;
		private String value;

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withLocale(String locale)
		{
			this.locale = locale;
			return this;
		}

		public Builder withValue(String value)
		{
			this.value = value;
			return this;
		}

		public DBMessage build()
		{
			return new DBMessage(this);
		}
	}

}
