/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

import io.imunity.scim.MockGroupMembershipData;
import io.imunity.scim.MockGroupStructuralData;
import io.imunity.scim.SCIMTestHelper;
import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityTaV;

@RunWith(MockitoJUnitRunner.class)
public class UserRetrievalServiceTest
{
	@Mock
	private MessageSource msg;
	@Mock
	private UserAuthzService authzMan;
	@Mock
	private EntityManagement entityManagement;
	@Mock
	private BulkGroupQueryService bulkService;
	@Mock
	private AttributesManagement attributesManagement;

	private UserRetrievalService userRetrievalService;

	@Before
	public void init()
	{
		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https//localhost:2443/scim"),
				"/scim",
				List.of("/scim", "/scim/Members1", "/scim/Members1/Subgroup1", "/scim/Members1/Subgroup2",
						"/scim/Members2", "/scim/Members2/Subgroup1", "/scim/Members2/Subgroup2"),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		userRetrievalService = new UserRetrievalService(authzMan, entityManagement, bulkService, attributesManagement,
				configuration);
	}

	@Test
	public void shouldReturnSingleUser() throws EngineException
	{
		Entity entity = addEntityWithTwoIdentitiesToMemebersSubgroup();
		addTwoMembersGroupsWithSubgroups();
		AttributeExt attribute1 = new AttributeExt(
				new Attribute("a", StringAttributeSyntax.ID, "/scim", List.of("a1v")), false);
		when(attributesManagement.getAttributes(eq(new EntityParam(entity.getId())), eq("/scim"), any()))
				.thenReturn(List.of(attribute1));
		when(entityManagement.getGroups(eq(new EntityParam(1L))))
				.thenReturn(Map.of("/scim", new GroupMembership("/scim", 1L, new Date()), "/scim/Members1/Subgroup1",
						new GroupMembership("/scim/Members1/Subgroup1", 1L, new Date()), "/scim/Members1",
						new GroupMembership("/scim/Members1", 1L, new Date())));

		User user = userRetrievalService.getUser(new PersistentId("1"));

		assertThat(user.entityId, is(1L));
		assertThat(user.identities.size(), is(2));
		assertThat(user.identities, hasItems(entity.getIdentities().get(0), entity.getIdentities().get(1)));
		assertThat(user.groups, hasItems(SCIMTestHelper.getGroupContent("/scim/Members1/Subgroup1").getGroup(),
				SCIMTestHelper.getGroupContent("/scim/Members1").getGroup()));
		assertThat(user.attributes, hasItems(attribute1));

	}

	@Test(expected = UserNotFoundException.class)
	public void shouldThrowExceptionWhenNotInScimRootGroup() throws EngineException
	{
		addEntityWithTwoIdentitiesToMemebersSubgroup();
		when(entityManagement.getGroups(eq(new EntityParam(1L)))).thenReturn(
				Map.of("/scim2/Members1/Subgroup1", new GroupMembership("/scim2/Members1/Subgroup1", 1L, new Date()),
						"/scim2/Members1", new GroupMembership("/scim2/Members1", 1L, new Date())));

		userRetrievalService.getUser(new PersistentId("1"));
	}

	private Entity addEntityWithTwoIdentitiesToMemebersSubgroup() throws EngineException
	{
		EntityInformation entityInformation = new EntityInformation();
		entityInformation.setId(1);
		Identity persitentIdentity = new Identity(PersistentIdentity.ID, "1", 1, "1");
		Date persitentIdCreation = new Date();
		Date persitentIdUpdate = Date.from(Instant.now().plusSeconds(2000));
		persitentIdentity.setCreationTs(persitentIdCreation);
		persitentIdentity.setUpdateTs(persitentIdUpdate);

		Identity emailIdentity = new Identity(EmailIdentity.ID, "email@email.com", 1, "email@email.com");
		Date emailIdCreation = new Date();
		Date emailIdUpdate = Date.from(Instant.now().plusSeconds(2000));
		emailIdentity.setCreationTs(emailIdCreation);
		emailIdentity.setUpdateTs(emailIdUpdate);

		Entity entity = new Entity(List.of(persitentIdentity, emailIdentity), entityInformation, null);

		when(entityManagement.getEntity(eq(new EntityParam(new IdentityTaV(PersistentIdentity.ID, "1")))))
				.thenReturn(entity);

		return entity;
	}

	@Test
	public void shouldReturnUsersFromRootGroup() throws EngineException
	{

		addTwoMembersGroupsWithSubgroups();
		EntityInGroupData entity1 = new EntityInGroupData(SCIMTestHelper.createPersitentEntity("0", 0), null,
				Set.of("/scim", "/scim/Members1", "/scim/Members1/Subgroup1"), new HashMap<>(), null, null);
		EntityInGroupData entity2 = new EntityInGroupData(SCIMTestHelper.createPersitentEntity("1", 1), null,
				Set.of("/scim", "/scim/Members2"), new HashMap<>(), null, null);

		GroupMembershipData membershipData = new MockGroupMembershipData();
		when(bulkService.getBulkMembershipData(eq("/"))).thenReturn(membershipData);
		when(bulkService.getMembershipInfo(eq(membershipData))).thenReturn(ImmutableMap.of(0l, entity1, 1l, entity2));
		AttributeExt attribute1 = new AttributeExt(
				new Attribute("a", StringAttributeSyntax.ID, "/scim", List.of("a1v")), false);
		when(bulkService.getGroupUsersAttributes(eq("/scim"), eq(membershipData)))
				.thenReturn(Map.of(1l, Map.of("a", attribute1), 0l, Map.of("a", attribute1)));

		List<User> users = userRetrievalService.getUsers();

		assertThat(users.size(), is(2));
		assertThat(users,
				hasItems(
						User.builder().withEntityId(1L)
								.withGroups(Set.of(SCIMTestHelper.getGroupContent("/scim").getGroup(),
										SCIMTestHelper.getGroupContent("/scim/Members2").getGroup()))
								.withIdentities(entity2.entity.getIdentities()).withAttributes(List.of(attribute1))

								.build(),
						User.builder().withEntityId(0L)
								.withGroups(Set.of(SCIMTestHelper.getGroupContent("/scim").getGroup(),
										SCIMTestHelper.getGroupContent("/scim/Members1").getGroup(),
										SCIMTestHelper.getGroupContent("/scim/Members1/Subgroup1").getGroup()))
								.withIdentities(entity1.entity.getIdentities()).withAttributes(List.of(attribute1))

								.build()));

	}

	@Test
	public void shouldReturnUsersWithoutAttributesInScimRootGroup() throws EngineException
	{
		addTwoMembersGroupsWithSubgroups();
		EntityInGroupData entity1 = new EntityInGroupData(SCIMTestHelper.createPersitentEntity("0", 0), null,
				Set.of("/scim", "/scim/Members1/Subgroup1"), new HashMap<>(), null, null);
		GroupMembershipData membershipData = new MockGroupMembershipData();
		when(bulkService.getBulkMembershipData(eq("/"))).thenReturn(membershipData);
		when(bulkService.getMembershipInfo(eq(membershipData))).thenReturn(ImmutableMap.of(0l, entity1));
		when(bulkService.getGroupUsersAttributes(eq("/scim"), eq(membershipData))).thenReturn(Collections.emptyMap());

		List<User> users = userRetrievalService.getUsers();

		assertThat(users.size(), is(1));
		assertThat(users, hasItems(
				User.builder().withEntityId(0L)
						.withGroups(Set.of(SCIMTestHelper.getGroupContent("/scim").getGroup(),
								SCIMTestHelper.getGroupContent("/scim/Members1/Subgroup1").getGroup()))
						.withIdentities(entity1.entity.getIdentities()).build()));

	}

	@Test
	public void shouldSkipUsersWithoutScimRootGroup() throws EngineException
	{
		addTwoMembersGroupsWithSubgroups();
		EntityInGroupData entity1 = new EntityInGroupData(SCIMTestHelper.createPersitentEntity("0", 0), null,
				Set.of("/scim2"), new HashMap<>(), null, null);
		EntityInGroupData entity2 = new EntityInGroupData(SCIMTestHelper.createPersitentEntity("1", 1), null,
				Set.of("/scim", "/scim/Members2"), new HashMap<>(), null, null);
		GroupMembershipData membershipData = new MockGroupMembershipData();
		when(bulkService.getBulkMembershipData(eq("/"))).thenReturn(membershipData);
		when(bulkService.getMembershipInfo(eq(membershipData))).thenReturn(ImmutableMap.of(0l, entity1, 1l, entity2));
		when(bulkService.getGroupUsersAttributes(eq("/scim"), eq(membershipData))).thenReturn(Collections.emptyMap());

		List<User> users = userRetrievalService.getUsers();

		assertThat(users.size(), is(1));
		assertThat(users,
				hasItems(User.builder().withEntityId(1L)
						.withGroups(Set.of(SCIMTestHelper.getGroupContent("/scim").getGroup(),
								SCIMTestHelper.getGroupContent("/scim/Members2").getGroup()))
						.withIdentities(entity2.entity.getIdentities()).build()));

	}

	private void addTwoMembersGroupsWithSubgroups() throws EngineException
	{
		GroupStructuralData gdata = new MockGroupStructuralData();
		when(bulkService.getBulkStructuralData(eq("/"))).thenReturn(gdata);
		Map<String, GroupContents> groupsWithSubgroups = new HashMap<>();
		groupsWithSubgroups.put("/", SCIMTestHelper.getGroupContent("/", List.of("/scim")));
		groupsWithSubgroups.put("/scim",
				SCIMTestHelper.getGroupContent("/scim", List.of("/scim/Members1", "/scim/Members2")));
		groupsWithSubgroups.put("/scim/Members1",
				SCIMTestHelper.getGroupContent("/scim/Members1", List.of("/scim/Members1/Subgroup1")));
		groupsWithSubgroups.put("/scim/Members1/Subgroup1", SCIMTestHelper.getGroupContent("/scim/Members1/Subgroup1"));
		groupsWithSubgroups.put("/scim/Members2", SCIMTestHelper.getGroupContent("/scim/Members2"));

		when(bulkService.getGroupAndSubgroups(eq(gdata))).thenReturn(groupsWithSubgroups);
	}
}
