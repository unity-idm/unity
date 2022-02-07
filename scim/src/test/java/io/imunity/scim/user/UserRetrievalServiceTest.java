/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
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
import io.imunity.scim.user.UserGroup.GroupType;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
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

	private UserRetrievalService userRetrievalService;

	@Before
	public void init()
	{
		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https//localhost:2443/scim"),
				"/scim", List.of("/scim/Members1", "/scim/Members2"));
		userRetrievalService = new UserRetrievalService(msg, authzMan, entityManagement, bulkService, configuration);
	}

	@Test
	public void shouldThrowExceptionWhenUserIsNotMember() throws EngineException
	{
		Entity entity = SCIMTestHelper.createPersitentEntity("1", 1);
		when(entityManagement.getEntity(eq(new EntityParam(new IdentityTaV(PersistentIdentity.ID, "1")))))
				.thenReturn(entity);
		Map<String, GroupMembership> groups = new HashMap<>();
		groups.put("/", null);
		when(entityManagement.getGroups(new EntityParam(1L))).thenReturn(groups);
		
		Throwable error = Assertions.catchThrowable(() -> userRetrievalService.getUser(new PersistentId("1")));
		Assertions.assertThat(error).isInstanceOf(UserNotFoundException.class);
	}

	@Test
	public void shouldReturnSingleUser() throws EngineException
	{
		Entity entity = addEntityWithTwoIdentitiesToMemebersSubgroup();
		addTwoMembersGroupsWithSubgroups();

		User user = userRetrievalService.getUser(new PersistentId("1"));

		assertThat(user.entityId, is(1L));
		assertThat(user.identities.size(), is(2));
		assertThat(user.identities,
				hasItems(
						UserIdentity.builder().withCreationTs(entity.getIdentities().get(0).getCreationTs().toInstant())
								.withUpdateTs(entity.getIdentities().get(0).getUpdateTs().toInstant())
								.withTypeId(PersistentIdentity.ID).withValue("1").build(),
						UserIdentity.builder().withCreationTs(entity.getIdentities().get(1).getCreationTs().toInstant())
								.withUpdateTs(entity.getIdentities().get(1).getUpdateTs().toInstant())
								.withTypeId(EmailIdentity.ID).withValue("email@email.com").build()));
		assertThat(user.groups,
				hasItems(
						UserGroup.builder().withValue("/scim/Members1/Subgroup1").withDisplayName("Subgroup1")
								.withType(GroupType.direct).build(),
						UserGroup.builder().withValue("/scim/Members1").withDisplayName("Members1")
								.withType(GroupType.direct).build()));
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

		Map<String, GroupMembership> groups = new HashMap<>();
		groups.put("/scim/Members1", null);
		groups.put("/scim/Members1/Subgroup1", null);

		when(entityManagement.getGroups(new EntityParam(1L))).thenReturn(groups);

		return entity;
	}

	@Test
	public void shouldReturnUsersFromAllMembersGroups() throws EngineException
	{

		addTwoMembersGroupsWithSubgroups();
		EntityInGroupData entity1 = new EntityInGroupData(SCIMTestHelper.createPersitentEntity("0", 0), null,
				Set.of("/scim/Members1/Subgroup1"), new HashMap<>(), null, null);
		EntityInGroupData entity2 = new EntityInGroupData(SCIMTestHelper.createPersitentEntity("1", 1), null,
				Set.of("/scim/Members2"), new HashMap<>(), null, null);

		GroupMembershipData membershipData = new MockGroupMembershipData();
		when(bulkService.getBulkMembershipData(eq("/"))).thenReturn(membershipData);
		when(bulkService.getMembershipInfo(eq(membershipData))).thenReturn(ImmutableMap.of(0l, entity1, 1l, entity2));

		List<User> users = userRetrievalService.getUsers();

		assertThat(users.size(), is(2));

		assertThat(users,
				hasItems(
						User.builder().withEntityId(1L)
								.withGroups(Set.of(UserGroup
										.builder().withType(GroupType.direct).withDisplayName("Members2")
										.withValue("/scim/Members2").build()))
								.withIdentities(List.of(UserIdentity.builder().withValue("1")
										.withTypeId(PersistentIdentity.ID)
										.withCreationTs(
												entity2.entity.getIdentities().get(0).getCreationTs().toInstant())
										.withUpdateTs(entity2.entity.getIdentities().get(0).getUpdateTs().toInstant())
										.build()))
								.build(),
						User.builder().withEntityId(0L)
								.withGroups(Set.of(UserGroup
										.builder().withType(GroupType.direct).withDisplayName("Subgroup1")
										.withValue("/scim/Members1/Subgroup1").build()))
								.withIdentities(List.of(UserIdentity.builder().withValue("0")
										.withTypeId(PersistentIdentity.ID)
										.withCreationTs(
												entity1.entity.getIdentities().get(0).getCreationTs().toInstant())
										.withUpdateTs(entity1.entity.getIdentities().get(0).getUpdateTs().toInstant())
										.build()))
								.build()));

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
