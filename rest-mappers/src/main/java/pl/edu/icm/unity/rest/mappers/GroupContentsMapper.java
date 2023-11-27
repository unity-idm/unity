/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers;

import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.rest.api.types.basic.RestGroupContents;
import pl.edu.icm.unity.types.basic.GroupContents;

public class GroupContentsMapper
{
	public static RestGroupContents map(GroupContents groupContents)
	{
		return RestGroupContents.builder()
				.withGroup(Optional.ofNullable(groupContents.getGroup())
						.map(GroupMapper::map)
						.orElse(null))
				.withMembers(groupContents.getMembers()
						.stream()
						.map(GroupMembershipMapper::map)
						.collect(Collectors.toList()))
				.withSubGroups(groupContents.getSubGroups())
				.build();
	}

	static GroupContents map(RestGroupContents restGroupContents)
	{
		GroupContents groupContents = new GroupContents();
		groupContents.setGroup(Optional.ofNullable(restGroupContents.group)
				.map(GroupMapper::map)
				.orElse(null));
		groupContents.setMembers(restGroupContents.members.stream()
				.map(GroupMembershipMapper::map)
				.collect(Collectors.toList()));
		groupContents.setSubGroups(restGroupContents.subGroups);
		return groupContents;
	}
}
