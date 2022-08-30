/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

import io.imunity.scim.MockGroupMembershipData;
import io.imunity.scim.MockGroupStructuralData;
import io.imunity.scim.SCIMTestHelper;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.group.GroupMember.MemberType;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.GroupContents;

@RunWith(MockitoJUnitRunner.class)
public class GroupRetrievalServiceTest
{
	@Mock
	private GroupAuthzService authzMan;
	@Mock
	private MessageSource msg;
	@Mock
	private GroupsManagement groupsMan;
	@Mock
	private BulkGroupQueryService bulkService;
	@Mock
	private AttributeSupport attrSupport;

	private GroupRetrievalService groupRetrievalService;

	@Test
	public void shouldThrowExceptionWhenRequestedGroupIsNotExposed() throws EngineException
	{
		SCIMEndpointDescription configuration = SCIMEndpointDescription.builder()
				.withBaseLocation(URI.create("https://localhost:2443/scim")).withRootGroup("/scim")
				.withMembershipGroups(List.of("/scim/Members1")).build();

		groupRetrievalService = new GroupRetrievalService(msg, authzMan, groupsMan, bulkService, attrSupport,
				configuration);

		when(groupsMan.isPresent(eq("/scim/NotMemberGroup"))).thenReturn(true);
		Throwable error = Assertions
				.catchThrowable(() -> groupRetrievalService.getGroup(new GroupId("/scim/NotMemberGroup")));
		Assertions.assertThat(error).isInstanceOf(GroupNotFoundException.class);
	}

	@Test
	public void shouldReturnOnlyGroupsOfWhichUserIsMember() throws EngineException
	{
		SCIMEndpointDescription configuration = SCIMEndpointDescription.builder()
				.withBaseLocation(URI.create("https://localhost:2443/scim")).withRootGroup("/scim")
				.withMembershipGroups(
						List.of("/scim", "/scim/Members1", "/scim/Members1/Subgroup1", "/scim/Members1/Subgroup2"))
				.build();

		groupRetrievalService = new GroupRetrievalService(msg, authzMan, groupsMan, bulkService, attrSupport,
				configuration);

		// given
		when(authzMan.getFilter()).thenReturn(s -> s.equals("/scim/Members1/Subgroup1") ? true : false);
		addTwoMembersGroupWithSubgroups();

		// when
		List<GroupData> groupData = groupRetrievalService.getGroups();

		// then
		assertThat(groupData.size(), is(1));
		assertThat(groupData.stream().map(g -> g.id).collect(Collectors.toSet()), hasItems("/scim/Members1/Subgroup1"));
	}

	@Test
	public void returnedGroupShouldHaveGroupsAndUserMembers() throws EngineException
	{
		SCIMEndpointDescription configuration = SCIMEndpointDescription.builder()
				.withBaseLocation(URI.create("https://localhost:2443/scim")).withRootGroup("/scim")
				.withMembershipGroups(List.of("/scim")).build();

		groupRetrievalService = new GroupRetrievalService(msg, authzMan, groupsMan, bulkService, attrSupport,
				configuration);

		when(groupsMan.isPresent(eq("/scim/Members1"))).thenReturn(true);
		when(authzMan.getFilter()).thenReturn(s -> true);

		addEntityNameAttrType();

		EntityInGroupData entity1 = new EntityInGroupData(SCIMTestHelper.createPersitentEntity("0", 0), null, null,
				new HashMap<>(), null, null);

		GroupMembershipData membershipData = new MockGroupMembershipData();
		when(bulkService.getBulkMembershipData(eq("/scim/Members1"))).thenReturn(membershipData);
		when(bulkService.getMembershipInfo(eq(membershipData))).thenReturn(ImmutableMap.of(0l, entity1));

		GroupStructuralData gdata = new MockGroupStructuralData();
		when(bulkService.getBulkStructuralData(eq("/"))).thenReturn(gdata);
		Map<String, GroupContents> allGroupsWithSubgroups = new HashMap<>();
		allGroupsWithSubgroups.put("/scim", SCIMTestHelper.getGroupContent("/scim", List.of("/scim/Members1")));
		allGroupsWithSubgroups.put("/scim/Members1",
				SCIMTestHelper.getGroupContent("/scim/Members1", List.of("/scim/Members1/Subgroup1")));
		allGroupsWithSubgroups.put("/scim/Members1/Subgroup1",
				SCIMTestHelper.getGroupContent("/scim/Members1/Subgroup1"));
		when(bulkService.getGroupAndSubgroups(eq(gdata))).thenReturn(allGroupsWithSubgroups);

		Map<String, GroupContents> subgroupContent = new HashMap<>();
		subgroupContent.put("/scim/Members1",
				SCIMTestHelper.getGroupContent("/scim/Members1", List.of("/scim/Members1/Subgroup1")));
		subgroupContent.put("/scim/Members1/Subgroup1", SCIMTestHelper.getGroupContent("/scim/Members1/Subgroup1"));

		when(bulkService.getGroupAndSubgroups(eq(gdata), eq("/scim/Members1"))).thenReturn(subgroupContent);

		// when
		GroupData groupData = groupRetrievalService.getGroup(new GroupId("/scim/Members1"));

		// then
		assertThat(groupData.members.size(), is(2));
		assertThat(groupData.members.stream().filter(m -> m.type.equals(MemberType.User)).collect(Collectors.toSet()),
				hasItems(GroupMember.builder().withType(MemberType.User).withValue("0").build()));

		assertThat(groupData.members.stream().filter(m -> m.type.equals(MemberType.Group)).collect(Collectors.toSet()),
				hasItems(GroupMember.builder().withType(MemberType.Group).withDisplayName("Subgroup1")
						.withValue("/scim/Members1/Subgroup1").build()));

	}

	@Test
	public void shouldRespectEffectiveMembershipGroupsWithWildcardExclusion() throws EngineException
	{
		// given
		SCIMEndpointDescription configuration = SCIMEndpointDescription.builder()
				.withBaseLocation(URI.create("https://localhost:2443/scim")).withRootGroup("/scim")
				.withMembershipGroups(List.of("/")).withExcludedMembershipGroups(List.of("/A*")).build();

		groupRetrievalService = new GroupRetrievalService(msg, authzMan, groupsMan, bulkService, attrSupport,
				configuration);

		when(authzMan.getFilter()).thenReturn(s -> true);
		addEntityNameAttrType();
		GroupStructuralData gdata1 = new MockGroupStructuralData();
		when(bulkService.getBulkStructuralData(eq("/"))).thenReturn(gdata1);
		Map<String, GroupContents> groupsWithSubgroups1 = new HashMap<>();
		groupsWithSubgroups1.put("/", SCIMTestHelper.getGroupContent("/", Collections.emptyList()));
		groupsWithSubgroups1.put("/A", SCIMTestHelper.getGroupContent("/A", Collections.emptyList()));
		groupsWithSubgroups1.put("/AA", SCIMTestHelper.getGroupContent("/AA", Collections.emptyList()));
		groupsWithSubgroups1.put("/B", SCIMTestHelper.getGroupContent("/B", Collections.emptyList()));
		
		when(bulkService.getGroupAndSubgroups(eq(gdata1))).thenReturn(groupsWithSubgroups1);

		// when
		List<GroupData> groupData = groupRetrievalService.getGroups();

		// then
		assertThat(groupData.size(), is(2));
		assertThat(groupData.stream().map(g -> g.id).collect(Collectors.toSet()), hasItems("/",  "/B"));

	
	}

	@Test
	public void shouldRespectEffectiveMembershipGroupsWithNormalGroupExclusion() throws EngineException
	{
		// given
		SCIMEndpointDescription configuration = SCIMEndpointDescription.builder()
				.withBaseLocation(URI.create("https://localhost:2443/scim")).withRootGroup("/scim")
				.withMembershipGroups(List.of("/")).withExcludedMembershipGroups(List.of("/A/B")).build();

		groupRetrievalService = new GroupRetrievalService(msg, authzMan, groupsMan, bulkService, attrSupport,
				configuration);

		when(authzMan.getFilter()).thenReturn(s -> true);
		addEntityNameAttrType();
		GroupStructuralData gdata1 = new MockGroupStructuralData();
		when(bulkService.getBulkStructuralData(eq("/"))).thenReturn(gdata1);
		Map<String, GroupContents> groupsWithSubgroups1 = new HashMap<>();
		groupsWithSubgroups1.put("/", SCIMTestHelper.getGroupContent("/", List.of("/A", "/D")));
		groupsWithSubgroups1.put("/A", SCIMTestHelper.getGroupContent("/A", List.of("/B", "/Bar")));		
		groupsWithSubgroups1.put("/A/B", SCIMTestHelper.getGroupContent("/A/B", List.of("/C")));
		groupsWithSubgroups1.put("/A/B/C", SCIMTestHelper.getGroupContent("/A/B/C", Collections.emptyList()));
		when(bulkService.getGroupAndSubgroups(eq(gdata1))).thenReturn(groupsWithSubgroups1);

		// when
		List<GroupData> groupData = groupRetrievalService.getGroups();

		// then
		assertThat(groupData.size(), is(2));
		assertThat(groupData.stream().map(g -> g.id).collect(Collectors.toSet()), hasItems("/", "/A"));
	}

	private void addTwoMembersGroupWithSubgroups() throws EngineException
	{
		GroupStructuralData gdata1 = new MockGroupStructuralData();
		when(bulkService.getBulkStructuralData(eq("/"))).thenReturn(gdata1);
		Map<String, GroupContents> groupsWithSubgroups1 = new HashMap<>();

		groupsWithSubgroups1.put("/scim",
				SCIMTestHelper.getGroupContent("/scim", List.of("/scim/Members1", "/scim/Members2")));

		groupsWithSubgroups1.put("/scim/Members1", SCIMTestHelper.getGroupContent("/scim/Members1",
				List.of("/scim/Members1/Subgroup1", "/scim/Members1/Subgroup2")));
		groupsWithSubgroups1.put("/scim/Members1/Subgroup1",
				SCIMTestHelper.getGroupContent("/scim/Members1/Subgroup1"));
		groupsWithSubgroups1.put("/scim/Members1/Subgroup2",
				SCIMTestHelper.getGroupContent("/scim/Members1/Subgroup2"));

		groupsWithSubgroups1.put("/scim/Members2", SCIMTestHelper.getGroupContent("/scim/Members2",
				List.of("/scim/Members2/Subgroup1", "/scim/Members2/Subgroup2")));
		groupsWithSubgroups1.put("/scim/Members2/Subgroup1",
				SCIMTestHelper.getGroupContent("/scim/Members2/Subgroup1"));
		groupsWithSubgroups1.put("/scim/Members2/Subgroup2",
				SCIMTestHelper.getGroupContent("/scim/Members2/Subgroup2"));

		when(bulkService.getGroupAndSubgroups(eq(gdata1))).thenReturn(groupsWithSubgroups1);
	}

	@Test
	public void returnedGroupsShouldHaveGroupsAndUserMembers() throws EngineException
	{
		// given
		SCIMEndpointDescription configuration = SCIMEndpointDescription.builder()
				.withBaseLocation(URI.create("https://localhost:2443/scim")).withRootGroup("/scim")
				.withMembershipGroups(List.of("/scim")).build();

		groupRetrievalService = new GroupRetrievalService(msg, authzMan, groupsMan, bulkService, attrSupport,
				configuration);

		when(authzMan.getFilter()).thenReturn(s -> true);
		addEntityNameAttrType();
		addTwoUserMembersToMembersSubgroups();
		addTwoMembersWithAttributeToAttrGroup();
		addTwoMembersGroupWithSubgroups();

		// when
		List<GroupData> groupData = groupRetrievalService.getGroups();

		// then
		assertThat(groupData.size(), is(7));
		assertThat(groupData.stream().map(g -> g.id).collect(Collectors.toSet()),
				hasItems("/scim/Members1", "/scim/Members1/Subgroup1", "/scim/Members2", "/scim/Members2/Subgroup1"));

		GroupData memberGroup1 = groupData.stream().filter(g -> g.id.equals("/scim/Members1")).findFirst().get();
		assertThat(memberGroup1.displayName, is("Members1"));
		assertThat(memberGroup1.members.size(), is(3));
		assertThat(
				memberGroup1.members.stream().filter(m -> m.type.equals(MemberType.User)).collect(Collectors.toSet()),
				hasItems(GroupMember.builder().withDisplayName("User0").withType(MemberType.User).withValue("0")
						.build()));
		assertThat(
				memberGroup1.members.stream().filter(m -> m.type.equals(MemberType.Group)).collect(Collectors.toSet()),
				hasItems(GroupMember.builder().withDisplayName("Subgroup1").withType(MemberType.Group)
						.withValue("/scim/Members1/Subgroup1").build()));

		GroupData member1SubGroup = groupData.stream().filter(g -> g.id.equals("/scim/Members1/Subgroup1")).findFirst()
				.get();
		assertThat(member1SubGroup.displayName, is("Subgroup1"));
		assertThat(member1SubGroup.members.size(), is(1));
		assertThat(
				member1SubGroup.members.stream().filter(m -> m.type.equals(MemberType.User))
						.collect(Collectors.toSet()),
				hasItems(GroupMember.builder().withDisplayName("User0").withType(MemberType.User).withValue("0")
						.build()));

		GroupData memberGroup2 = groupData.stream().filter(g -> g.id.equals("/scim/Members2")).findFirst().get();
		assertThat(memberGroup2.displayName, is("Members2"));
		assertThat(memberGroup2.members.size(), is(3));
		assertThat(
				memberGroup2.members.stream().filter(m -> m.type.equals(MemberType.User)).collect(Collectors.toSet()),
				hasItems(GroupMember.builder().withDisplayName("User1").withType(MemberType.User).withValue("1")
						.build()));
		assertThat(
				memberGroup2.members.stream().filter(m -> m.type.equals(MemberType.Group)).collect(Collectors.toSet()),
				hasItems(GroupMember.builder().withDisplayName("Subgroup1").withType(MemberType.Group)
						.withValue("/scim/Members2/Subgroup1").build()));

		GroupData member2SubGroup1 = groupData.stream().filter(g -> g.id.equals("/scim/Members2/Subgroup1")).findFirst()
				.get();
		assertThat(member2SubGroup1.displayName, is("Subgroup1"));
		assertThat(member2SubGroup1.members.size(), is(1));
		assertThat(
				member2SubGroup1.members.stream().filter(m -> m.type.equals(MemberType.User))
						.collect(Collectors.toSet()),
				hasItems(GroupMember.builder().withDisplayName("User1").withType(MemberType.User).withValue("1")
						.build()));

	}

	private void addTwoMembersWithAttributeToAttrGroup() throws IllegalIdentityValueException, EngineException
	{
		EntityInGroupData entityWithDispAttrData1 = new EntityInGroupData(SCIMTestHelper.createPersitentEntity("0", 0),
				null, null,
				ImmutableMap.of("disp", new AttributeExt(StringAttribute.of("disp", "/scim", "User0"), true)), null,
				null);
		EntityInGroupData entityWithDispAttrData2 = new EntityInGroupData(SCIMTestHelper.createPersitentEntity("1", 1),
				null, null,
				ImmutableMap.of("disp", new AttributeExt(StringAttribute.of("disp", "/scim", "User1"), true)), null,
				null);
		GroupMembershipData data2 = new MockGroupMembershipData();
		when(bulkService.getBulkMembershipData(eq("/scim"))).thenReturn(data2);
		when(bulkService.getMembershipInfo(eq(data2)))
				.thenReturn(ImmutableMap.of(0l, entityWithDispAttrData1, 1l, entityWithDispAttrData2));
	}

	private void addTwoUserMembersToMembersSubgroups() throws IllegalIdentityValueException, EngineException
	{
		EntityInGroupData entity1 = new EntityInGroupData(SCIMTestHelper.createPersitentEntity("0", 0), null,
				Set.of("/", "/scim", "/scim/Members1", "/scim/Members1/Subgroup1"), new HashMap<>(), null, null);
		EntityInGroupData entity2 = new EntityInGroupData(SCIMTestHelper.createPersitentEntity("1", 1), null,
				Set.of("/", "/scim", "/scim/Members2", "/scim/Members2/Subgroup1"), new HashMap<>(), null, null);

		GroupMembershipData data1 = new MockGroupMembershipData();
		when(bulkService.getBulkMembershipData(eq("/"))).thenReturn(data1);
		when(bulkService.getMembershipInfo(eq(data1))).thenReturn(ImmutableMap.of(0l, entity1, 1l, entity2));
	}

	private void addEntityNameAttrType() throws EngineException
	{
		when(attrSupport.getAttributeTypeWithSingeltonMetadata(eq(EntityNameMetadataProvider.NAME)))
				.thenReturn(new AttributeType("disp", null));

	}
}
