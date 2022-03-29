/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.util.Objects;

class UserGroup
{
	enum GroupType
	{
		direct, indirect
	}

	final String value;
	final String displayName;
	final GroupType type;

	private UserGroup(Builder builder)
	{
		this.value = builder.value;
		this.displayName = builder.displayName;
		this.type = builder.type;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(displayName, type, value);
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
		UserGroup other = (UserGroup) obj;
		return Objects.equals(displayName, other.displayName) && type == other.type
				&& Objects.equals(value, other.value);
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private String value;
		private String displayName;
		private GroupType type;

		private Builder()
		{
		}

		Builder withValue(String value)
		{
			this.value = value;
			return this;
		}

		Builder withDisplayName(String displayName)
		{
			this.displayName = displayName;
			return this;
		}

		Builder withType(GroupType type)
		{
			this.type = type;
			return this;
		}

		UserGroup build()
		{
			return new UserGroup(this);
		}
	}
}
