/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

import java.util.List;

import java.util.Collections;

class SCIMGroup
{
	final String id;
	final String displayName;
	final List<SCIMGroupMember> members;

	private SCIMGroup(Builder builder)
	{
		this.id = builder.id;
		this.displayName = builder.displayName;
		this.members = builder.members;
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private String id;
		private String displayName;
		private List<SCIMGroupMember> members = Collections.emptyList();

		private Builder()
		{
		}

		Builder withId(String id)
		{
			this.id = id;
			return this;
		}

		Builder withDisplayName(String displayName)
		{
			this.displayName = displayName;
			return this;
		}

		Builder withMembers(List<SCIMGroupMember> members)
		{
			this.members = members;
			return this;
		}

		SCIMGroup build()
		{
			return new SCIMGroup(this);
		}
	}
}
