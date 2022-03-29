/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.GroupsWithMembers;
import pl.edu.icm.unity.engine.api.groupMember.GroupMemberService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
class GroupMemberOptimizedRESTService
{
	private final GroupMemberService groupMemberService;
	private final BulkGroupQueryService bulkQueryService;

	GroupMemberOptimizedRESTService(GroupMemberService groupMemberService,
	                                BulkGroupQueryService bulkQueryService) {
		this.groupMemberService = groupMemberService;
		this.bulkQueryService = bulkQueryService;
	}

	public List<SimpleGroupMember> getGroupMembers(String group, List<String> attributes) throws EngineException {
		Group groupObj = groupMemberService.getGroup(group);
		if(isBulkApiNeeded(groupObj, attributes)){
			return getBulkGroupMembers(group, attributes::contains);
		}
		return groupMemberService.getGroupMembers(group, attributes);
	}

	public Map<String, List<SimpleGroupMember>> getGroupMembers(List<String> groups, List<String> attributes) throws EngineException {
		boolean isBulkApiNeeded = groups.stream()
				.map(this::getGroup)
				.anyMatch(group -> isBulkApiNeeded(group, attributes));

		if(isBulkApiNeeded){
			getMultiBulkGroupMembers(groups, attributes);
		}
		return groupMemberService.getGroupMembers(groups, attributes);
	}

	private void getMultiBulkGroupMembers(List<String> groups, List<String> attributes)
	{
		GroupsWithMembers members = bulkQueryService.getMembersWithAttributeForAllGroups("/", new HashSet<>(groups));

		Map<String, List<SimpleGroupMember>> attributesByGroup = new HashMap<>();

		for (Map.Entry<String, List<pl.edu.icm.unity.engine.api.bulk.EntityGroupAttributes>> groupData: members.membersByGroup.entrySet())
		{
			List<SimpleGroupMember> perGroupAttributes = groupData.getValue().stream()
					.map(src ->
					{
						Entity entity = members.entities.get(src.entityId);
						Collection<AttributeExt> values = src.attribtues.values().stream()
								.filter(x -> attributes.contains(x.getName()))
								.collect(Collectors.toList());
						return new SimpleGroupMember(entity.getEntityInformation(), entity.getIdentities(), values);
					})
					.collect(Collectors.toList());
			attributesByGroup.put(groupData.getKey(), perGroupAttributes);
		}
	}

	private Group getGroup(String group)
	{
		try
		{
			return groupMemberService.getGroup(group);
		} catch (EngineException e)
		{
			throw new RuntimeException(e);
		}
	}

	private boolean isBulkApiNeeded(Group groupObj, List<String> attributes)
	{
		return Arrays.stream(groupObj.getAttributeStatements())
				.filter(AttributeStatement::dynamicAttributeMode)
				.map(AttributeStatement::getAssignedAttributeName)
				.anyMatch(attributes::contains);
	}

	private List<SimpleGroupMember> getBulkGroupMembers(String group, Predicate<String> attributesFilter) throws EngineException {
		GroupMembershipData bulkMembershipData = bulkQueryService.getBulkMembershipData(group);
		Map<Long, Map<String, AttributeExt>> userAttributes =
			bulkQueryService.getGroupUsersAttributes(group, bulkMembershipData);
		Map<Long, Entity> entitiesData = bulkQueryService.getGroupEntitiesNoContextWithoutTargeted(bulkMembershipData);
		List<SimpleGroupMember> ret = new ArrayList<>(userAttributes.size());
		for (Long memberId: userAttributes.keySet())
		{
			Collection<AttributeExt> attributes = userAttributes.get(memberId).values().stream()
					.filter(attributeExt -> attributesFilter.test(attributeExt.getName()))
					.collect(Collectors.toList());
			Entity entity = entitiesData.get(memberId);
			ret.add(new SimpleGroupMember(entity.getEntityInformation(), entity.getIdentities(), attributes));
		}
		return ret;
	}
}
