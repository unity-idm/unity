/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identitytype;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBEmailConfirmationConfiguration.Builder.class)
class DBEmailConfirmationConfiguration
{
	public final String messageTemplate;
	public final int validityTime;

	private DBEmailConfirmationConfiguration(Builder builder)
	{
		this.messageTemplate = builder.messageTemplate;
		this.validityTime = builder.validityTime;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(messageTemplate, validityTime);
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
		DBEmailConfirmationConfiguration other = (DBEmailConfirmationConfiguration) obj;
		return Objects.equals(messageTemplate, other.messageTemplate) && validityTime == other.validityTime;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String messageTemplate;
		private int validityTime;

		private Builder()
		{
		}

		public Builder withMessageTemplate(String messageTemplate)
		{
			this.messageTemplate = messageTemplate;
			return this;
		}

		public Builder withValidityTime(int validityTime)
		{
			this.validityTime = validityTime;
			return this;
		}

		public DBEmailConfirmationConfiguration build()
		{
			return new DBEmailConfirmationConfiguration(this);
		}
	}

}
