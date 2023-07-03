/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.groupMember;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.GroupsWithMembers;
import pl.edu.icm.unity.engine.api.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.engine.api.groupMember.GroupMemberWithAttributes;
import pl.edu.icm.unity.engine.api.groupMember.GroupMembersService;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
class GroupMembersAttributesServiceImpl implements GroupMembersService
{
	private final GroupMemberService groupMemberService;
	private final BulkGroupQueryService bulkQueryService;
	private final GroupDAO groupDAO;
	private final InternalAuthorizationManager authz;


	GroupMembersAttributesServiceImpl(GroupMemberService groupMemberService,
	                                  BulkGroupQueryService bulkQueryService,
	                                  GroupDAO groupDAO, InternalAuthorizationManager authz) {
		this.groupMemberService = groupMemberService;
		this.bulkQueryService = bulkQueryService;
		this.groupDAO = groupDAO;
		this.authz = authz;
	}

	@Override
	@Transactional
	public List<GroupMemberWithAttributes> getGroupMembersWithSelectedAttributes(String group, List<String> attributes)
	{
		authz.checkAuthorizationRT(AuthzCapability.readHidden, AuthzCapability.read);
		Group groupObj = groupDAO.get(group);

		if (isAnyOfAttributesDynamic(groupObj, attributes))
		{
			return getGroupMembersWithBulkAPI(group, attributes::contains);
		}
		return groupMemberService.getGroupMembersWithAttributes(group, attributes);
	}

	@Override
	@Transactional
	public Map<String, List<GroupMemberWithAttributes>> getGroupsMembersInGroupsWithSelectedAttributes(List<String> groups, List<String> attributes)
	{
		authz.checkAuthorizationRT(AuthzCapability.readHidden, AuthzCapability.read);
		boolean isAnyOfAttributesIsDynamic = groups.stream()
				.map(groupDAO::get)
				.anyMatch(group -> isAnyOfAttributesDynamic(group, attributes));

		if(isAnyOfAttributesIsDynamic)
		{
			return getMultiBulkGroupMembers(groups, attributes);
		}
		return groupMemberService.getGroupMembersWithAttributes(groups, attributes);
	}

	private Map<String, List<GroupMemberWithAttributes>> getMultiBulkGroupMembers(List<String> groups, List<String> attributes)
	{
		GroupsWithMembers members = bulkQueryService.getMembersWithAttributeForAllGroups("/", new HashSet<>(groups));

		Map<String, List<GroupMemberWithAttributes>> attributesByGroup = new HashMap<>();

		for (Map.Entry<String, List<pl.edu.icm.unity.engine.api.bulk.EntityGroupAttributes>> groupData: members.membersByGroup.entrySet())
		{
			List<GroupMemberWithAttributes> perGroupAttributes = groupData.getValue().stream()
					.map(src ->
					{
						Entity entity = members.entities.get(src.entityId);
						Collection<AttributeExt> values = src.attribtues.values().stream()
								.filter(x -> attributes.contains(x.getName()))
								.collect(Collectors.toList());
						return new GroupMemberWithAttributes(entity.getEntityInformation(), entity.getIdentities(), values);
					})
					.collect(Collectors.toList());
			attributesByGroup.put(groupData.getKey(), perGroupAttributes);
		}
		return attributesByGroup;
	}

	private boolean isAnyOfAttributesDynamic(Group groupObj, List<String> attributes)
	{
		return Arrays.stream(groupObj.getAttributeStatements())
				.map(AttributeStatement::getAssignedAttributeName)
				.anyMatch(attributes::contains);
	}

	private List<GroupMemberWithAttributes> getGroupMembersWithBulkAPI(String group, Predicate<String> attributesFilter)
	{
		GroupMembershipData bulkMembershipData = null;
		try
		{
			bulkMembershipData = bulkQueryService.getBulkMembershipData(group);
		} catch (EngineException e)
		{
			throw new RuntimeEngineException(e);
		}
		Map<Long, Map<String, AttributeExt>> userAttributes =
			bulkQueryService.getGroupUsersAttributes(group, bulkMembershipData);
		Map<Long, Entity> entitiesData = bulkQueryService.getGroupEntitiesNoContextWithoutTargeted(bulkMembershipData);
		List<GroupMemberWithAttributes> ret = new ArrayList<>(userAttributes.size());
		for (Long memberId: userAttributes.keySet())
		{
			Collection<AttributeExt> attributes = userAttributes.get(memberId).values().stream()
					.filter(attributeExt -> attributesFilter.test(attributeExt.getName()))
					.collect(Collectors.toList());
			Entity entity = entitiesData.get(memberId);
			ret.add(new GroupMemberWithAttributes(entity.getEntityInformation(), entity.getIdentities(), attributes));
		}
		return ret;
	}
}
