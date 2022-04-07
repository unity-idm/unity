/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.rest.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides details about members of groups.
 */
public class RestMultiGroupMembersWithAttributes
{
	public Map<String, List<RestGroupMemberWithAttributes>> members;

	public RestMultiGroupMembersWithAttributes(Map<String, List<RestGroupMemberWithAttributes>> members)
	{
		this.members = Map.copyOf(members);
	}

	public RestMultiGroupMembersWithAttributes()
	{
	}

	public Map<String, List<RestGroupMemberWithAttributes>> getMembers()
	{
		return members;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestMultiGroupMembersWithAttributes that = (RestMultiGroupMembersWithAttributes) o;
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
