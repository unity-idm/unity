/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.notify;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBNotificationChannel.Builder.class)
class DBNotificationChannel
{
	public final String name;
	public final String description;
	public final String configuration;
	public final String facilityId;

	private DBNotificationChannel(Builder builder)
	{
		this.name = builder.name;
		this.description = builder.description;
		this.configuration = builder.configuration;
		this.facilityId = builder.facilityId;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(configuration, description, facilityId, name);
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
		DBNotificationChannel other = (DBNotificationChannel) obj;
		return Objects.equals(configuration, other.configuration) && Objects.equals(description, other.description)
				&& Objects.equals(facilityId, other.facilityId) && Objects.equals(name, other.name);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private String description;
		private String configuration;
		private String facilityId;

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withDescription(String description)
		{
			this.description = description;
			return this;
		}

		public Builder withConfiguration(String configuration)
		{
			this.configuration = configuration;
			return this;
		}

		public Builder withFacilityId(String facilityId)
		{
			this.facilityId = facilityId;
			return this;
		}

		public DBNotificationChannel build()
		{
			return new DBNotificationChannel(this);
		}
	}

}
