/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.group.SCIMGroupAuthzService.SCIMGroupAuthzServiceFactory;
import io.imunity.scim.group.SCIMGroupMember.Builder;
import io.imunity.scim.group.SCIMGroupMember.MemberType;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;

class SCIMGroupRetrievalService
{
	static final String DEFAULT_META_VERSION = "v1";
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, SCIMGroupRetrievalService.class);

	private final SCIMGroupAuthzService authzMan;
	private final MessageSource msg;
	private final GroupsManagement groupsMan;
	private final BulkGroupQueryService bulkService;
	private final AttributeSupport attrSupport;
	private final SCIMEndpointDescription configuration;

	SCIMGroupRetrievalService(MessageSource msg, SCIMGroupAuthzService authzMan, GroupsManagement groupMan,
			BulkGroupQueryService bulkService, AttributeSupport attrSupport, SCIMEndpointDescription configuration)
	{
		this.msg = msg;
		this.authzMan = authzMan;
		this.groupsMan = groupMan;
		this.bulkService = bulkService;
		this.configuration = configuration;
		this.attrSupport = attrSupport;
	}

	SCIMGroup getGroup(GroupId groupId) throws EngineException
	{
		authzMan.checkReadGroups();
		assertIsMembershipGroup(groupId.id);

		Map<Long, EntityInGroupData> membershipInfo = bulkService
				.getMembershipInfo(bulkService.getBulkMembershipData(groupId.id));
		Map<String, GroupContents> groupAndSubgroups = bulkService
				.getGroupAndSubgroups(bulkService.getBulkStructuralData(groupId.id));

		GroupContents main = groupAndSubgroups.get(groupId.id);
		Optional<String> nameAttribute = getNameAttribute();

		List<SCIMGroupMember> members = new ArrayList<>();
		membershipInfo.values().forEach(m -> members.add(mapToUserMember(m, nameAttribute)));
		groupAndSubgroups.values().stream().filter(g -> !g.equals(main))
				.forEach(g -> members.add(mapToGroupMemeber(g)));

		return SCIMGroup.builder().withDisplayName(main.getGroup().getDisplayedNameShort(msg).getValue(msg))
				.withId(main.getGroup().getPathEncoded()).withMembers(members).build();
	}

	private Optional<String> getNameAttribute() throws EngineException
	{
		AttributeType attributeTypeWithSingeltonMetadata = attrSupport
				.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME);
		return Optional.ofNullable(
				attributeTypeWithSingeltonMetadata != null ? attributeTypeWithSingeltonMetadata.getName() : null);
	}

	List<SCIMGroup> getGroups() throws EngineException
	{
		authzMan.checkReadGroups();

		List<SCIMGroup> groups = new ArrayList<>();
		Map<Long, EntityInGroupData> membershipInfo = bulkService
				.getMembershipInfo(bulkService.getBulkMembershipData("/"));
		Optional<String> nameAttribute = getNameAttribute();

		for (String configuredMemebershipGroup : configuration.membershipGroups)
		{
			Map<String, GroupContents> groupAndSubgroups = bulkService
					.getGroupAndSubgroups(bulkService.getBulkStructuralData(configuredMemebershipGroup));
			GroupContents main = groupAndSubgroups.get(configuredMemebershipGroup);
			fillMembersAndAddGroupResource(main, nameAttribute, membershipInfo, groupAndSubgroups, groups);
		}

		return groups;
	}

	private void fillMembersAndAddGroupResource(GroupContents group, Optional<String> nameAttribute,
			Map<Long, EntityInGroupData> membershipInfo, Map<String, GroupContents> groupAndSubgroups,
			List<SCIMGroup> groups)
	{
		List<SCIMGroupMember> members = new ArrayList<>();
		membershipInfo.values().stream().filter(e -> e.groups.contains(group.getGroup().getPathEncoded()))
				.forEach(e -> members.add(mapToUserMember(e, nameAttribute)));
		groupAndSubgroups.values().stream()
				.filter(g -> Group.isDirectChild(g.getGroup().getPathEncoded(), group.getGroup().getPathEncoded()))
				.forEach(g ->
				{
					members.add(mapToGroupMemeber(g));
					fillMembersAndAddGroupResource(g, nameAttribute, membershipInfo, groupAndSubgroups, groups);
				});

		groups.add(SCIMGroup.builder().withDisplayName(group.getGroup().getDisplayedNameShort(msg).getValue(msg))
				.withId(group.getGroup().getPathEncoded()).withMembers(members).build());
	}

	private SCIMGroupMember mapToGroupMemeber(GroupContents group)
	{
		return SCIMGroupMember.builder().withValue(group.getGroup().getPathEncoded()).withType(MemberType.Group)
				.withDisplayName(group.getGroup().getDisplayedNameShort(msg).getValue(msg)).build();
	}

	private SCIMGroupMember mapToUserMember(EntityInGroupData entityInGroupData, Optional<String> nameAttribute)
	{
		Identity persistence = entityInGroupData.entity.getIdentities().stream()
				.filter(i -> i.getTypeId().equals(PersistentIdentity.ID)).findFirst().get();

		Builder userMember = SCIMGroupMember.builder().withValue(persistence.getComparableValue())
				.withType(MemberType.User);

		if (nameAttribute.isPresent())
		{
			AttributeExt displayedName = entityInGroupData.rootAttributesByName.get(nameAttribute.get());
			if (displayedName != null && displayedName.getValues().size() > 0)
			{
				userMember.withDisplayName(displayedName.getValues().get(0));
			}
		}
		return userMember.build();
	}

	private void assertIsMembershipGroup(String group) throws EngineException
	{
		if (groupsMan.isPresent(group))
		{
			for (String configuredMemebershipGroups : configuration.membershipGroups)
			{
				if (Group.isChildOrSame(group, configuredMemebershipGroups))
				{
					return;
				}
			}
		}
		log.error("Group " + group + " is out of range for configured membership groups");
		throw new GroupNotFoundException("Invalid group");
	}

	@Component
	class SCIMGroupRetrievalServiceFactory
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

		SCIMGroupRetrievalService getService(SCIMEndpointDescription configuration)
		{
			return new SCIMGroupRetrievalService(msg, authzManFactory.getService(configuration), groupMan,
					bulkService, attrSupport, configuration);
		}
	}

}
