/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.hamcrest.CoreMatchers.is;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.imunity.scim.common.ListResponse;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.config.SchemaType;
import io.imunity.scim.config.SchemaWithMapping;
import io.imunity.scim.user.mapping.evaluation.UserSchemaEvaluator;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;

@RunWith(MockitoJUnitRunner.class)
public class UserAssemblyServiceTest
{

	private UserAssemblyService assemblyService;

	@Mock
	private UserSchemaEvaluator userSchemaEvaluator;
	@Mock
	private GroupsManagement groupsManagement;

	@Before
	public void init()
	{
		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https://localhost:2443/scim"),
				"/scim", List.of("/scim/Members1", "/scim/Members2"),
				List.of(SchemaWithMapping.builder().withType(SchemaType.USER_CORE).withName("UserCore").withId("UC")
						.withEnable(true).build(),
						SchemaWithMapping.builder().withType(SchemaType.USER).withName("UserExt").withId("UE")
								.withEnable(true).build())

		);
		assemblyService = new UserAssemblyService(configuration, userSchemaEvaluator, groupsManagement);
	}

	@Test
	public void shouldAssemblySingleUserAndEvalEnabledSchemas() throws MalformedURLException, EngineException
	{
		User user = User.builder().withEntityId(0L)
				.withGroups(Set.of(new Group("/scim/Members1"), new Group("/scim/Members2")))
				.withIdentities(List.of(new Identity(PersistentIdentity.ID, "0", 0l, "0"))).build();

		SCIMUserResource userRes = assemblyService.mapToUserResource(user);

		verify(userSchemaEvaluator).evalUserSchema(eq(user),
				eq(SchemaWithMapping.builder().withType(SchemaType.USER_CORE).withName("UserCore").withId("UC")
						.withEnable(true).build()),
				eq(List.of(SchemaWithMapping.builder().withType(SchemaType.USER).withName("UserExt").withId("UE")
						.withEnable(true).build())),
				any());

		assertThat(userRes.id, is("0"));
		assertThat(userRes.schemas, hasItems("UC", "UE"));
		assertThat(userRes.meta.location.toURL().toExternalForm(), is("https://localhost:2443/scim/Users/0"));
	}

	@Test
	public void shouldAssemblyMultiUsersAndEvalEnabledSchemas() throws MalformedURLException, EngineException
	{
		User user1 = User.builder().withEntityId(1L)
				.withGroups(Set.of(new Group("/scim/Members1"), new Group("/scim/Members2")))
				.withIdentities(List.of(new Identity(PersistentIdentity.ID, "1", 1l, "0"))).build();
		User user2 = User.builder().withEntityId(2L)
				.withGroups(Set.of(new Group("/scim/Members1"), new Group("/scim/Members2")))
				.withIdentities(List.of(new Identity(PersistentIdentity.ID, "2", 2l, "0"))).build();

		ListResponse<SCIMUserResource> userRes = assemblyService.mapToListUsersResource(List.of(user1, user2));

		verify(userSchemaEvaluator).evalUserSchema(eq(user1),
				eq(SchemaWithMapping.builder().withType(SchemaType.USER_CORE).withName("UserCore").withId("UC")
						.withEnable(true).build()),
				eq(List.of(SchemaWithMapping.builder().withType(SchemaType.USER).withName("UserExt").withId("UE")
						.withEnable(true).build())),
				any());

		verify(userSchemaEvaluator).evalUserSchema(eq(user2),
				eq(SchemaWithMapping.builder().withType(SchemaType.USER_CORE).withName("UserCore").withId("UC")
						.withEnable(true).build()),
				eq(List.of(SchemaWithMapping.builder().withType(SchemaType.USER).withName("UserExt").withId("UE")
						.withEnable(true).build())),
				any());
		assertThat(userRes.totalResults, is(2));
		assertThat(userRes.resources.stream().map(r -> r.id).collect(Collectors.toList()), hasItems("1", "2"));
	}

	@Test
	public void shouldUserCreationAndUpdateTimeFromIdentities() throws MalformedURLException, EngineException
	{
		Date creation = Date.from(Instant.now());
		Date update = Date.from(Instant.now());

		Identity id1 = new Identity(PersistentIdentity.ID, "0", 0l, "0");
		id1.setCreationTs(creation);
		id1.setUpdateTs(update);
		Identity id2 = new Identity(EmailIdentity.ID, "email@email.com", 0l, "email@email.com");
		id2.setCreationTs(Date.from(creation.toInstant().minusSeconds(1500)));
		id2.setUpdateTs(Date.from(update.toInstant().minusSeconds(1000)));

		User user = User.builder().withEntityId(1L).withIdentities(List.of(id1, id2)).build();

		SCIMUserResource mappedUser = assemblyService.mapToUserResource(user);
		assertThat(mappedUser.meta.created, is(creation.toInstant()));
		assertThat(mappedUser.meta.lastModified, is(update.toInstant()));

	}
//
//	@Test
//	public void shouldAsesemlyFullSingleUser() throws MalformedURLException
//	{
//		Instant creation = Instant.now();
//		Instant update = Instant.now();
//		User user = User.builder().withEntityId(1L)
//				.withGroups(Set.of(
//						UserGroup.builder().withValue("/scim/Members1").withDisplayName("Members1")
//								.withType(GroupType.direct).build(),
//						UserGroup.builder().withValue("/scim/Members2").withDisplayName("Members2")
//								.withType(GroupType.direct).build()))
//				.withIdentities(List.of(
//						UserIdentity.builder().withCreationTs(creation).withUpdateTs(update)
//								.withTypeId(PersistentIdentity.ID).withValue("1").build(),
//						UserIdentity.builder().withCreationTs(creation.minusSeconds(1500))
//								.withUpdateTs(update.minusSeconds(1000)).withTypeId(EmailIdentity.ID)
//								.withValue("email@email.com").build()))
//				.build();
//
//		SCIMUserResource mappedUser = assemblyService.mapToSingleUserResource(user);
//
//		assertThat(mappedUser.id, is("1"));
//		assertThat(mappedUser.meta.location.toURL().toExternalForm(), is("https://localhost:2443/scim/Users/1"));
//		assertThat(mappedUser.meta.resourceType.toString(), is("User"));
//		assertThat(mappedUser.meta.created, is(creation));
//		assertThat(mappedUser.meta.lastModified, is(update));
//		assertThat(mappedUser.groups,
//				hasItems(
//						SCIMUserGroupResource.builder().withType(GroupType.direct.toString())
//								.withValue("/scim/Members1").withDisplay("Members1")
//								.withRef(URI.create("https://localhost:2443/scim/Groups/%2Fscim%2FMembers1")).build(),
//						SCIMUserGroupResource.builder().withType(GroupType.direct.toString())
//								.withValue("/scim/Members2").withDisplay("Members2")
//								.withRef(URI.create("https://localhost:2443/scim/Groups/%2Fscim%2FMembers2")).build()));
//
//		assertThat(mappedUser.userName, is("email@email.com"));
//
//	}
//
//	@Test
//	public void shouldAsesemlyListOfUsers() throws MalformedURLException
//	{
//		Instant now = Instant.now();
//
//		User user1 = User.builder().withEntityId(1L)
//				.withGroups(Set.of(
//						UserGroup.builder().withValue("/scim/Members1").withDisplayName("Members1")
//								.withType(GroupType.direct).build(),
//						UserGroup.builder().withValue("/scim/Members2").withDisplayName("Members2")
//								.withType(GroupType.direct).build()))
//				.withIdentities(List.of(
//						UserIdentity.builder().withCreationTs(now).withUpdateTs(now).withTypeId(PersistentIdentity.ID)
//								.withValue("1").build(),
//						UserIdentity.builder().withCreationTs(now).withUpdateTs(now).withTypeId(EmailIdentity.ID)
//								.withValue("email@email.com").build()))
//				.build();
//		User user2 = User.builder().withEntityId(2L)
//				.withGroups(Set.of(UserGroup.builder().withValue("/scim/Members2").withDisplayName("Members2")
//						.withType(GroupType.direct).build()))
//				.withIdentities(List.of(
//						UserIdentity.builder().withCreationTs(now).withUpdateTs(now).withTypeId(PersistentIdentity.ID)
//								.withValue("2").build(),
//						UserIdentity.builder().withCreationTs(now).withUpdateTs(now).withTypeId(EmailIdentity.ID)
//								.withValue("email2@email.com").build()))
//				.build();
//
//		ListResponse<SCIMUserResource> mappedUsers = assemblyService.mapToListUsersResource(List.of(user1, user2));
//
//		SCIMUserResource mappedUser1 = mappedUsers.resources.stream().filter(u -> u.id.equals("1")).findAny().get();
//		assertThat(mappedUser1.id, is("1"));
//		assertThat(mappedUser1.meta.location.toURL().toExternalForm(), is("https://localhost:2443/scim/Users/1"));
//		assertThat(mappedUser1.meta.resourceType.toString(), is("User"));
//		assertThat(mappedUser1.groups,
//				hasItems(
//						SCIMUserGroupResource.builder().withType(GroupType.direct.toString())
//								.withValue("/scim/Members1").withDisplay("Members1")
//								.withRef(URI.create("https://localhost:2443/scim/Groups/%2Fscim%2FMembers1")).build(),
//						SCIMUserGroupResource.builder().withType(GroupType.direct.toString())
//								.withValue("/scim/Members2").withDisplay("Members2")
//								.withRef(URI.create("https://localhost:2443/scim/Groups/%2Fscim%2FMembers2")).build()));
//
//		assertThat(mappedUser1.userName, is("email@email.com"));
//
//		SCIMUserResource mappedUser2 = mappedUsers.resources.stream().filter(u -> u.id.equals("2")).findAny().get();
//		assertThat(mappedUser2.id, is("2"));
//		assertThat(mappedUser2.meta.location.toURL().toExternalForm(), is("https://localhost:2443/scim/Users/2"));
//		assertThat(mappedUser2.meta.resourceType.toString(), is("User"));
//		assertThat(mappedUser2.groups,
//				hasItems(SCIMUserGroupResource.builder().withType(GroupType.direct.toString())
//						.withValue("/scim/Members2").withDisplay("Members2")
//						.withRef(URI.create("https://localhost:2443/scim/Groups/%2Fscim%2FMembers2")).build()));
//
//		assertThat(mappedUser2.userName, is("email2@email.com"));
//
//	}
}
