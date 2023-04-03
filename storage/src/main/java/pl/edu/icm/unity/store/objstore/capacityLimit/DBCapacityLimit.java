/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.capacityLimit;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBCapacityLimit.Builder.class)
class DBCapacityLimit
{
	public final String name;
	public final int value;

	private DBCapacityLimit(Builder builder)
	{
		this.name = builder.name;
		this.value = builder.value;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, value);
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
		DBCapacityLimit other = (DBCapacityLimit) obj;
		return Objects.equals(name, other.name) && value == other.value;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private int value;

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withValue(int value)
		{
			this.value = value;
			return this;
		}

		public DBCapacityLimit build()
		{
			return new DBCapacityLimit(this);
		}
	}

}
