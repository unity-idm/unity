/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

import java.util.List;
import java.util.Objects;
import java.util.Collections;

class GroupData
{
	final String id;
	final String displayName;
	final List<GroupMember> members;

	private GroupData(Builder builder)
	{
		this.id = builder.id;
		this.displayName = builder.displayName;
		this.members = List.copyOf(builder.members);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(displayName, id, members);
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
		GroupData other = (GroupData) obj;
		return Objects.equals(displayName, other.displayName) && Objects.equals(id, other.id)
				&& Objects.equals(members, other.members);
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private String id;
		private String displayName;
		private List<GroupMember> members = Collections.emptyList();

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

		Builder withMembers(List<GroupMember> members)
		{
			this.members = members;
			return this;
		}

		GroupData build()
		{
			return new GroupData(this);
		}
	}
}
