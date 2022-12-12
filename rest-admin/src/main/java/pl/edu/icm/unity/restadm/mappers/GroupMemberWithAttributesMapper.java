/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.rest.api.RestGroupMemberWithAttributes;
import pl.edu.icm.unity.engine.api.groupMember.GroupMemberWithAttributes;

public class GroupMemberWithAttributesMapper
{
	public static RestGroupMemberWithAttributes map(GroupMemberWithAttributes groupMemberWithAttributes)
	{
		return RestGroupMemberWithAttributes.builder()
				.withEntityInformation(EntityInformationMapper.map(groupMemberWithAttributes.getEntityInformation()))
				.withAttributes(groupMemberWithAttributes.getAttributes()
						.stream()
						.map(a -> Optional.ofNullable(a).map(AttributeExtMapper::map).orElse(null))
						.collect(Collectors.toList()))
				.withIdentities(groupMemberWithAttributes.getIdentities()
						.stream()
						.map(i -> IdentityMapper.map(i))
						.collect(Collectors.toList()))
				.build();
	}

	public static GroupMemberWithAttributes map(RestGroupMemberWithAttributes restGroupMemberWithAttributes)
	{
		return new GroupMemberWithAttributes(
				EntityInformationMapper.map(restGroupMemberWithAttributes.entityInformation),
				restGroupMemberWithAttributes.identities.stream()
						.map(IdentityMapper::map)
						.collect(Collectors.toList()),
				restGroupMemberWithAttributes.attributes.stream()
						.map(a -> Optional.ofNullable(a).map(AttributeExtMapper::map).orElse(null))
						.collect(Collectors.toList()));

	}
}
