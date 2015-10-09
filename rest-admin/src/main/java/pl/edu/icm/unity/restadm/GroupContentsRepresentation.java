/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.util.List;
import java.util.stream.Collectors;

import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Simple wrapper removing information which is not exposed yet. Will be removed in future.
 * @author K. Benedyczak
 */
public class GroupContentsRepresentation
{
	private List<String> subGroups;
	private List<Long> members;

	public GroupContentsRepresentation(GroupContents full)
	{
		this.subGroups = full.getSubGroups();
		this.members = full.getMembers().stream().
				map(GroupMembership::getEntityId).
				collect(Collectors.toList());
	}

	public List<String> getSubGroups()
	{
		return subGroups;
	}

	public List<Long> getMembers()
	{
		return members;
	}
}