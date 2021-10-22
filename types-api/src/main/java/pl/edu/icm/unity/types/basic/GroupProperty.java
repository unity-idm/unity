/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GroupProperty
{
	public static final int MAX_KEY_LENGTH = 50;
	public static final int MAX_VALUE_LENGHT = 1024;

	public final String key;
	public final String value;

	public GroupProperty(@JsonProperty("key") String key, @JsonProperty("value") String value)
	{
		if (Objects.isNull(key) || key.length() > MAX_KEY_LENGTH)
		{
			throw new IllegalArgumentException("key is too long. Max is " + MAX_KEY_LENGTH);
		}
		if (Objects.nonNull(value) && value.length() > MAX_VALUE_LENGHT)
		{
			throw new IllegalArgumentException("value is too long. Max is " + MAX_VALUE_LENGHT);
		}

		this.key = key;
		this.value = value;
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
		GroupProperty other = (GroupProperty) obj;
		return Objects.equals(key, other.key) && Objects.equals(value, other.value);
	}
}
