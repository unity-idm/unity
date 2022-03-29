/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides details about members of groups.
 */
public class SimpleMultiGroupMembers
{
	public Map<String, List<SimpleGroupMember>> members;

	public SimpleMultiGroupMembers(Map<String, List<SimpleGroupMember>> members)
	{
		this.members = members;
	}

	public SimpleMultiGroupMembers()
	{
	}

	public Map<String, List<SimpleGroupMember>> getMembers()
	{
		return members;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SimpleMultiGroupMembers that = (SimpleMultiGroupMembers) o;
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
