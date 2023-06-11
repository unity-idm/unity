/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.imunity.scim.MembershipGroupsUtils;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.group.GroupAuthzService.SCIMGroupAuthzServiceFactory;
import io.imunity.scim.group.GroupMember.Builder;
import io.imunity.scim.group.GroupMember.MemberType;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;

class GroupRetrievalService
{
	static final String DEFAULT_META_VERSION = "v1";
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, GroupRetrievalService.class);

	private final GroupAuthzService authzMan;
	private final MessageSource msg;
	private final GroupsManagement groupsMan;
	private final BulkGroupQueryService bulkService;
	private final AttributeSupport attrSupport;
	private final SCIMEndpointDescription configuration;

	GroupRetrievalService(MessageSource msg, GroupAuthzService authzMan, GroupsManagement groupMan,
			BulkGroupQueryService bulkService, AttributeSupport attrSupport, SCIMEndpointDescription configuration)
	{
		this.msg = msg;
		this.authzMan = authzMan;
		this.groupsMan = groupMan;
		this.bulkService = bulkService;
		this.configuration = configuration;
		this.attrSupport = attrSupport;
	}

	GroupData getGroup(GroupId groupId) throws EngineException
	{
		authzMan.checkReadGroups();
		GroupStructuralData bulkStructuralData = bulkService.getBulkStructuralData("/");
		Map<String, GroupContents> allGroupsWithSubgroups = bulkService.getGroupAndSubgroups(bulkStructuralData);
		List<String> effectiveMembershipGroups = MembershipGroupsUtils.getEffectiveMembershipGroups(
				configuration.membershipGroups, configuration.excludedMembershipGroups, allGroupsWithSubgroups.values()
						.stream().collect(Collectors.toMap(g -> g.getGroup().getPathEncoded(), g -> g.getGroup())));

		assertIsMembershipGroup(groupId.id, effectiveMembershipGroups);

		if (!authzMan.getFilter().test(groupId.id))
		{
			throw new GroupNotFoundException("Invalid group " + groupId.id);
		}

		Map<Long, EntityInGroupData> membershipInfo = bulkService
				.getMembershipInfo(bulkService.getBulkMembershipData(groupId.id));
		Map<Long, EntityInGroupData> attributesInfo = bulkService
				.getMembershipInfo(bulkService.getBulkMembershipData(configuration.rootGroup));

		Map<String, GroupContents> groupAndSubgroups = bulkService.getGroupAndSubgroups(bulkStructuralData, groupId.id);

		GroupContents main = groupAndSubgroups.get(groupId.id);
		Optional<String> nameAttribute = getNameAttribute();

		List<GroupMember> members = new ArrayList<>();
		membershipInfo.values().forEach(m -> members
				.add(mapToUserMember(m, Optional.ofNullable(attributesInfo.get(m.entity.getId())), nameAttribute)));
		groupAndSubgroups.values().stream()
				.filter(g -> !g.equals(main) && effectiveMembershipGroups.contains(g.getGroup().getPathEncoded()))
				.forEach(g -> members.add(mapToGroupMemeber(g)));

		return GroupData.builder().withDisplayName(main.getGroup().getDisplayedNameShort(msg).getValue(msg))
				.withId(main.getGroup().getPathEncoded()).withMembers(members).build();
	}

	private Optional<String> getNameAttribute() throws EngineException
	{
		AttributeType attributeTypeWithSingeltonMetadata = attrSupport
				.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME);
		return Optional.ofNullable(
				attributeTypeWithSingeltonMetadata != null ? attributeTypeWithSingeltonMetadata.getName() : null);
	}

	List<GroupData> getGroups() throws EngineException
	{
		authzMan.checkReadGroups();

		List<GroupData> groups = new ArrayList<>();
		Map<Long, EntityInGroupData> membershipInfo = bulkService
				.getMembershipInfo(bulkService.getBulkMembershipData("/"));
		Optional<String> nameAttribute = getNameAttribute();

		Map<Long, EntityInGroupData> attributesInfo = bulkService
				.getMembershipInfo(bulkService.getBulkMembershipData(configuration.rootGroup));

		Map<String, GroupContents> allGroupsWithSubgroups = bulkService
				.getGroupAndSubgroups(bulkService.getBulkStructuralData("/"));
		Predicate<String> filter = authzMan.getFilter();

		List<String> effectiveMembershipGroups = MembershipGroupsUtils.getEffectiveMembershipGroups(
				configuration.membershipGroups, configuration.excludedMembershipGroups, allGroupsWithSubgroups.values()
						.stream().collect(Collectors.toMap(g -> g.getGroup().getPathEncoded(), g -> g.getGroup())));

		for (String configuredMemebershipGroup : effectiveMembershipGroups.stream().filter(filter).sorted()
				.collect(Collectors.toList()))
		{
			GroupContents main = allGroupsWithSubgroups.get(configuredMemebershipGroup);
			if (main == null)
			{
				log.warn("Can not get configured membership group " + configuredMemebershipGroup);
				continue;
			}
			fillMembersAndAddGroupResource(main, allGroupsWithSubgroups, nameAttribute, membershipInfo, attributesInfo,
					effectiveMembershipGroups, groups);
		}

		return groups;
	}

	private void fillMembersAndAddGroupResource(GroupContents group, Map<String, GroupContents> groupAndSubgroups,
			Optional<String> nameAttribute, Map<Long, EntityInGroupData> membershipInfo,
			Map<Long, EntityInGroupData> membershipInfoForAttr, List<String> effectiveMembershipGroups,
			List<GroupData> groups)
	{
		List<GroupMember> members = new ArrayList<>();
		membershipInfo.values().stream().filter(e -> e.groups.contains(group.getGroup().getPathEncoded()))
				.forEach(e -> members.add(mapToUserMember(e,
						Optional.ofNullable(membershipInfoForAttr.get(e.entity.getId())), nameAttribute)));
		groupAndSubgroups.values().stream()
				.filter(g -> Group.isDirectChild(g.getGroup().getPathEncoded(), group.getGroup().getPathEncoded())
						&& effectiveMembershipGroups.contains(g.getGroup().getPathEncoded()))
				.forEach(g ->
				{
					members.add(mapToGroupMemeber(g));
				});

		groups.add(GroupData.builder().withDisplayName(group.getGroup().getDisplayedNameShort(msg).getValue(msg))
				.withId(group.getGroup().getPathEncoded()).withMembers(members).build());
	}

	private GroupMember mapToGroupMemeber(GroupContents group)
	{
		return GroupMember.builder().withValue(group.getGroup().getPathEncoded()).withType(MemberType.Group)
				.withDisplayName(group.getGroup().getDisplayedNameShort(msg).getValue(msg)).build();
	}

	private GroupMember mapToUserMember(EntityInGroupData entityInGroupData,
			Optional<EntityInGroupData> entityInGroupDataForAttr, Optional<String> nameAttribute)
	{
		Identity persistence = entityInGroupData.entity.getIdentities().stream()
				.filter(i -> i.getTypeId().equals(PersistentIdentity.ID)).findFirst().get();

		Builder userMember = GroupMember.builder().withValue(persistence.getComparableValue())
				.withType(MemberType.User);

		if (nameAttribute.isPresent() && entityInGroupDataForAttr.isPresent())
		{
			AttributeExt displayedName = entityInGroupDataForAttr.get().groupAttributesByName.get(nameAttribute.get());
			if (displayedName != null && displayedName.getValues().size() > 0)
			{
				userMember.withDisplayName(displayedName.getValues().get(0));
			}
		}
		return userMember.build();
	}

	private void assertIsMembershipGroup(String group, List<String> effectiveMembershipGroups) throws EngineException
	{
		if (groupsMan.isPresent(group))
		{
			if (effectiveMembershipGroups.stream().anyMatch(g -> g.equals(group)))
			{
				return;
			}
		}
		log.error("Group " + group + " is out of range for configured membership groups");
		throw new GroupNotFoundException("Invalid group " + group);
	}

	@Component
	static class SCIMGroupRetrievalServiceFactory
	{
		private final GroupsManagement groupMan;
		private final BulkGroupQueryService bulkService;
		private final SCIMGroupAuthzServiceFactory authzManFactory;
		private final MessageSource msg;
		private final AttributeSupport attrSupport;

		@Autowired
		SCIMGroupRetrievalServiceFactory(MessageSource msg, @Qualifier("insecure") GroupsManagement groupMan,
				@Qualifier("insecure") BulkGroupQueryService bulkService, SCIMGroupAuthzServiceFactory authzManFactory,
				AttributeSupport attrSupport)
		{
			this.groupMan = groupMan;
			this.bulkService = bulkService;
			this.authzManFactory = authzManFactory;
			this.msg = msg;
			this.attrSupport = attrSupport;
		}

		GroupRetrievalService getService(SCIMEndpointDescription configuration)
		{
			return new GroupRetrievalService(msg, authzManFactory.getService(configuration), groupMan, bulkService,
					attrSupport, configuration);
		}
	}

}
