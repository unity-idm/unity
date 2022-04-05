/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.groupMember;
;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MultiGroupMembersWithAttributes
{
	public final Map<String, List<GroupMemberWithAttributes>> members;

	public MultiGroupMembersWithAttributes(Map<String, List<GroupMemberWithAttributes>> members)
	{
		this.members = Map.copyOf(members);
	}


	public Map<String, List<GroupMemberWithAttributes>> getMembers()
	{
		return members;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MultiGroupMembersWithAttributes that = (MultiGroupMembersWithAttributes) o;
		return Objects.equals(members, that.members);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(members);
	}

	@Override
	public String toString()
	{
		return "SimpleMultiGroupMembers{" +
				"members=" + members +
				'}';
	}
}
