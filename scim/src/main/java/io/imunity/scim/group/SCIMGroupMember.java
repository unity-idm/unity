/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

class SCIMGroupMember
{
	enum MemberType
	{
		Group, User
	};

	final String value;
	final String displayName;
	final MemberType type;

	private SCIMGroupMember(Builder builder)
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
		private MemberType type;

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

		Builder withType(MemberType type)
		{
			this.type = type;
			return this;
		}

		SCIMGroupMember build()
		{
			return new SCIMGroupMember(this);
		}
	}
}
