/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.services;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.exceptions.ResourceNotFoundException;
import io.imunity.scim.handlers.SCIMGroupHandler;
import io.imunity.scim.handlers.SCIMUserHandler;
import io.imunity.scim.messages.ListResponse;
import io.imunity.scim.types.GroupId;
import io.imunity.scim.types.GroupResource;
import io.imunity.scim.types.Member;
import io.imunity.scim.types.Meta;
import io.imunity.scim.types.Member.Builder;
import io.imunity.scim.types.Member.MemberType;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthorizationManagement;
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

public class SCIMGroupService
{
	public static final String DEFAULT_META_VERSION = "v1";
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, SCIMGroupService.class);

	private final AuthorizationManagement authzMan;
	private final MessageSource msg;
	private final GroupsManagement groupsMan;
	private final BulkGroupQueryService bulkService;
	private final AttributeSupport attrSupport;
	private final SCIMEndpointDescription configuration;

	public SCIMGroupService(MessageSource msg, AuthorizationManagement authzMan, GroupsManagement groupMan,
			BulkGroupQueryService bulkService, AttributeSupport attrSupport, SCIMEndpointDescription configuration)
	{
		this.msg = msg;
		this.authzMan = authzMan;
		this.groupsMan = groupMan;
		this.bulkService = bulkService;
		this.configuration = configuration;
		this.attrSupport = attrSupport;
	}

	public GroupResource getGroup(GroupId groupId) throws EngineException
	{
		authzMan.checkReadCapability(false, configuration.rootGroup);
		assertIsMembershipGroup(groupId.id);

		Map<Long, EntityInGroupData> membershipInfo = bulkService
				.getMembershipInfo(bulkService.getBulkMembershipData(groupId.id));
		Map<String, GroupContents> groupAndSubgroups = bulkService
				.getGroupAndSubgroups(bulkService.getBulkStructuralData(groupId.id));

		GroupContents main = groupAndSubgroups.get(groupId.id);
		Optional<String> nameAttribute = getNameAttribute();

		List<Member> members = new ArrayList<>();
		membershipInfo.values().forEach(m -> members.add(mapToUserMember(m, nameAttribute)));
		groupAndSubgroups.values().stream().filter(g -> !g.equals(main))
				.forEach(g -> members.add(mapToGroupMemeber(g)));

		return GroupResource.builder().withDisplayName(main.getGroup().getDisplayedNameShort(msg).getValue(msg))
				.withId(main.getGroup().getPathEncoded()).withMembers(members).build();

	}

	private Optional<String> getNameAttribute() throws EngineException
	{
		AttributeType attributeTypeWithSingeltonMetadata = attrSupport
				.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME);
		return Optional.ofNullable(
				attributeTypeWithSingeltonMetadata != null ? attributeTypeWithSingeltonMetadata.getName() : null);
	}

	public ListResponse<GroupResource> getGroups() throws EngineException
	{
		authzMan.checkReadCapability(false, configuration.rootGroup);
		
		List<GroupResource> groups = new ArrayList<>();
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

		return ListResponse.<GroupResource>builder().withResources(groups).withTotalResults(groups.size()).build();

	}

	private void fillMembersAndAddGroupResource(GroupContents group, Optional<String> nameAttribute,
			Map<Long, EntityInGroupData> membershipInfo, Map<String, GroupContents> groupAndSubgroups,
			List<GroupResource> groups)
	{
		List<Member> members = new ArrayList<>();
		membershipInfo.values().stream().filter(e -> e.groups.contains(group.getGroup().getPathEncoded()))
				.forEach(e -> members.add(mapToUserMember(e, nameAttribute)));
		groupAndSubgroups.values().stream()
				.filter(g -> Group.isDirectChild(g.getGroup().getPathEncoded(), group.getGroup().getPathEncoded()))
				.forEach(g ->
				{
					members.add(mapToGroupMemeber(g));
					fillMembersAndAddGroupResource(g, nameAttribute, membershipInfo, groupAndSubgroups, groups);
				});

		// FIXME creation and modification time in meta
		groups.add(GroupResource.builder()
				.withMeta(Meta.builder().withResourceType(Meta.ResourceType.Group).withVersion(DEFAULT_META_VERSION)
						// .withCreated(reated)
						// .withLastModified(lastModified)
						.withLocation(getGroupLocation(group.getGroup())).build())
				.withDisplayName(group.getGroup().getDisplayedNameShort(msg).getValue(msg))
				.withId(group.getGroup().getPathEncoded()).withMembers(members).build());
	}

	private Member mapToGroupMemeber(GroupContents group)
	{
		return Member.builder().withValue(group.getGroup().getPathEncoded()).withRef(getGroupLocation(group.getGroup()))
				.withType(MemberType.Group).withDisplay(group.getGroup().getDisplayedNameShort(msg).getValue(msg))
				.build();
	}

	private URI getGroupLocation(Group group)
	{
		return UriBuilder.fromUri(configuration.baseLocation).path(SCIMGroupHandler.SINGLE_GROUP_LOCATION)
				.path(URLEncoder.encode(group.getPathEncoded(), StandardCharsets.UTF_8)).build();
	}

	private Member mapToUserMember(EntityInGroupData entityInGroupData, Optional<String> nameAttribute)
	{
		Identity persistence = entityInGroupData.entity.getIdentities().stream()
				.filter(i -> i.getTypeId().equals(PersistentIdentity.ID)).findFirst().get();
		UriBuilder locationBuilder = UriBuilder.fromUri(configuration.baseLocation);
		locationBuilder.path(SCIMUserHandler.SINGLE_USER_LOCATION + "/" + persistence.getComparableValue());
		URI location = locationBuilder.build();

		Builder userMember = Member.builder().withValue(persistence.getComparableValue()).withRef(location)
				.withType(MemberType.User);
		if (nameAttribute.isPresent())
		{
			AttributeExt displayedName = entityInGroupData.rootAttributesByName.get(nameAttribute.get());
			if (displayedName != null && displayedName.getValues().size() > 0)
			{
				userMember.withDisplay(displayedName.getValues().get(0));
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
		throw new ResourceNotFoundException("Invalid group");
	}

	@Component
	public static class SCIMGroupServiceFactory
	{
		private final GroupsManagement groupMan;
		private final BulkGroupQueryService bulkService;
		private final AuthorizationManagement authzMan;
		private final MessageSource msg;
		private final AttributeSupport attrSupport;

		@Autowired
		public SCIMGroupServiceFactory(MessageSource msg, @Qualifier("insecure") GroupsManagement groupMan,
				@Qualifier("insecure") BulkGroupQueryService bulkService, AuthorizationManagement authzMan,
				AttributeSupport attrSupport)
		{
			this.groupMan = groupMan;
			this.bulkService = bulkService;
			this.authzMan = authzMan;
			this.msg = msg;
			this.attrSupport = attrSupport;
		}

		public SCIMGroupService getService(SCIMEndpointDescription configuration)
		{
			return new SCIMGroupService(msg, authzMan, groupMan, bulkService, attrSupport, configuration);
		}
	}
}
