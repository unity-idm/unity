/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

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
