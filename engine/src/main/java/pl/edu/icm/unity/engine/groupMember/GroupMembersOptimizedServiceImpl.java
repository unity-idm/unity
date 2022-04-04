/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.groupMember;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.GroupsWithMembers;
import pl.edu.icm.unity.engine.api.groupMember.GroupMemberWithAttributes;
import pl.edu.icm.unity.engine.api.groupMember.GroupMembersService;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.RuntimeAuthorizationException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Group;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
class GroupMembersOptimizedServiceImpl implements GroupMembersService
{
	private final GroupMemberServiceHelper groupMemberServiceHelper;
	private final BulkGroupQueryService bulkQueryService;
	private final GroupDAO groupDAO;
	private final InternalAuthorizationManager authz;


	GroupMembersOptimizedServiceImpl(GroupMemberServiceHelper groupMemberService,
	                                 BulkGroupQueryService bulkQueryService,
	                                 GroupDAO groupDAO, InternalAuthorizationManager authz) {
		this.groupMemberServiceHelper = groupMemberService;
		this.bulkQueryService = bulkQueryService;
		this.groupDAO = groupDAO;
		this.authz = authz;
	}

	@Override
	@Transactional
	public List<GroupMemberWithAttributes> getGroupsMembersWithSelectedAttributes(String group, List<String> attributes)
	{
		checkAuthorization();
		Group groupObj = groupDAO.get(group);

		if (isAnyOfAttributesIsDynamic(groupObj, attributes))
		{
			return getBulkGroupMembers(group, attributes::contains);
		}
		return groupMemberServiceHelper.getGroupMembers(group, attributes);
	}

	private void checkAuthorization()
	{
		try
		{
			authz.checkAuthorization(AuthzCapability.readHidden, AuthzCapability.read);
		} catch (AuthorizationException e)
		{
			throw new RuntimeAuthorizationException(e);
		}
	}

	@Override
	@Transactional
	public Map<String, List<GroupMemberWithAttributes>> getGroupsMembersInGroupsWithSelectedAttributes(List<String> groups, List<String> attributes)
	{
		checkAuthorization();
		boolean isAnyOfAttributesIsDynamic = groups.stream()
				.map(groupDAO::get)
				.anyMatch(group -> isAnyOfAttributesIsDynamic(group, attributes));

		if(isAnyOfAttributesIsDynamic)
		{
			return getMultiBulkGroupMembers(groups, attributes);
		}
		return groupMemberServiceHelper.getGroupMembers(groups, attributes);
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

	private boolean isAnyOfAttributesIsDynamic(Group groupObj, List<String> attributes)
	{
		return Arrays.stream(groupObj.getAttributeStatements())
				.filter(AttributeStatement::dynamicAttributeMode)
				.map(AttributeStatement::getAssignedAttributeName)
				.anyMatch(attributes::contains);
	}

	private List<GroupMemberWithAttributes> getBulkGroupMembers(String group, Predicate<String> attributesFilter)
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
