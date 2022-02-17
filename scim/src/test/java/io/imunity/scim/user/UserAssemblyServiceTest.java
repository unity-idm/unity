/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.imunity.scim.common.ListResponse;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.user.UserGroup.GroupType;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;

@RunWith(MockitoJUnitRunner.class)
public class UserAssemblyServiceTest
{

	private UserAssemblyService assemblyService;

	@Before
	public void init()
	{
		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https://localhost:2443/scim"),
				"/scim", List.of("/scim/Members1", "/scim/Members2"), Collections.emptyList());
		assemblyService = new UserAssemblyService(configuration);
	}

	@Test
	public void shouldUseNoneUsername() throws MalformedURLException
	{
		Instant now = Instant.now();
		User user = User.builder().withEntityId(1L)
				.withGroups(Set.of(
						UserGroup.builder().withValue("/scim/Members1").withDisplayName("Members1")
								.withType(GroupType.direct).build(),
						UserGroup.builder().withValue("/scim/Members2").withDisplayName("Members2")
								.withType(GroupType.direct).build()))
				.withIdentities(List.of(
						UserIdentity.builder().withCreationTs(now).withUpdateTs(now).withTypeId(PersistentIdentity.ID)
								.withValue("1").build()))
				.build();
		SCIMUserResource mappedUser = assemblyService.mapToSingleUserResource(user);

		assertThat(mappedUser.userName, is("none"));
	}
	
	@Test
	public void shouldUserCreationAndUpdateTimeFromIdentities() throws MalformedURLException
	{
		Instant creation = Instant.now();
		Instant update = Instant.now();

		User user = User.builder().withEntityId(1L)
				.withIdentities(List.of(
						UserIdentity.builder().withCreationTs(creation).withUpdateTs(update)
								.withTypeId(PersistentIdentity.ID).withValue("1").build(),
						UserIdentity.builder().withCreationTs(creation.minusSeconds(1500))
								.withUpdateTs(update.minusSeconds(1000)).withTypeId(EmailIdentity.ID)
								.withValue("email@email.com").build()))
				.build();

		SCIMUserResource mappedUser = assemblyService.mapToSingleUserResource(user);
		assertThat(mappedUser.meta.created, is(creation));
		assertThat(mappedUser.meta.lastModified, is(update));
	
	}

	@Test
	public void shouldAsesemlyFullSingleUser() throws MalformedURLException
	{
		Instant creation = Instant.now();
		Instant update = Instant.now();
		User user = User.builder().withEntityId(1L)
				.withGroups(Set.of(
						UserGroup.builder().withValue("/scim/Members1").withDisplayName("Members1")
								.withType(GroupType.direct).build(),
						UserGroup.builder().withValue("/scim/Members2").withDisplayName("Members2")
								.withType(GroupType.direct).build()))
				.withIdentities(List.of(
						UserIdentity.builder().withCreationTs(creation).withUpdateTs(update)
								.withTypeId(PersistentIdentity.ID).withValue("1").build(),
						UserIdentity.builder().withCreationTs(creation.minusSeconds(1500))
								.withUpdateTs(update.minusSeconds(1000)).withTypeId(EmailIdentity.ID)
								.withValue("email@email.com").build()))
				.build();

		SCIMUserResource mappedUser = assemblyService.mapToSingleUserResource(user);

		assertThat(mappedUser.id, is("1"));
		assertThat(mappedUser.meta.location.toURL().toExternalForm(), is("https://localhost:2443/scim/User/1"));
		assertThat(mappedUser.meta.resourceType.toString(), is("User"));
		assertThat(mappedUser.meta.created, is(creation));
		assertThat(mappedUser.meta.lastModified, is(update));
		assertThat(mappedUser.groups,
				hasItems(
						SCIMUserGroupResource.builder().withType(GroupType.direct.toString())
								.withValue("/scim/Members1").withDisplay("Members1")
								.withRef(URI.create("https://localhost:2443/scim/Group/%2Fscim%2FMembers1")).build(),
						SCIMUserGroupResource.builder().withType(GroupType.direct.toString())
								.withValue("/scim/Members2").withDisplay("Members2")
								.withRef(URI.create("https://localhost:2443/scim/Group/%2Fscim%2FMembers2")).build()));

		assertThat(mappedUser.userName, is("email@email.com"));

	}

	@Test
	public void shouldAsesemlyListOfUsers() throws MalformedURLException
	{
		Instant now = Instant.now();

		User user1 = User.builder().withEntityId(1L)
				.withGroups(Set.of(
						UserGroup.builder().withValue("/scim/Members1").withDisplayName("Members1")
								.withType(GroupType.direct).build(),
						UserGroup.builder().withValue("/scim/Members2").withDisplayName("Members2")
								.withType(GroupType.direct).build()))
				.withIdentities(List.of(
						UserIdentity.builder().withCreationTs(now).withUpdateTs(now).withTypeId(PersistentIdentity.ID)
								.withValue("1").build(),
						UserIdentity.builder().withCreationTs(now).withUpdateTs(now).withTypeId(EmailIdentity.ID)
								.withValue("email@email.com").build()))
				.build();
		User user2 = User.builder().withEntityId(2L)
				.withGroups(Set.of(UserGroup.builder().withValue("/scim/Members2").withDisplayName("Members2")
						.withType(GroupType.direct).build()))
				.withIdentities(List.of(
						UserIdentity.builder().withCreationTs(now).withUpdateTs(now).withTypeId(PersistentIdentity.ID)
								.withValue("2").build(),
						UserIdentity.builder().withCreationTs(now).withUpdateTs(now).withTypeId(EmailIdentity.ID)
								.withValue("email2@email.com").build()))
				.build();

		ListResponse<SCIMUserResource> mappedUsers = assemblyService.mapToListUsersResource(List.of(user1, user2));

		SCIMUserResource mappedUser1 = mappedUsers.resources.stream().filter(u -> u.id.equals("1")).findAny().get();
		assertThat(mappedUser1.id, is("1"));
		assertThat(mappedUser1.meta.location.toURL().toExternalForm(), is("https://localhost:2443/scim/User/1"));
		assertThat(mappedUser1.meta.resourceType.toString(), is("User"));
		assertThat(mappedUser1.groups,
				hasItems(
						SCIMUserGroupResource.builder().withType(GroupType.direct.toString())
								.withValue("/scim/Members1").withDisplay("Members1")
								.withRef(URI.create("https://localhost:2443/scim/Group/%2Fscim%2FMembers1")).build(),
						SCIMUserGroupResource.builder().withType(GroupType.direct.toString())
								.withValue("/scim/Members2").withDisplay("Members2")
								.withRef(URI.create("https://localhost:2443/scim/Group/%2Fscim%2FMembers2")).build()));

		assertThat(mappedUser1.userName, is("email@email.com"));

		SCIMUserResource mappedUser2 = mappedUsers.resources.stream().filter(u -> u.id.equals("2")).findAny().get();
		assertThat(mappedUser2.id, is("2"));
		assertThat(mappedUser2.meta.location.toURL().toExternalForm(), is("https://localhost:2443/scim/User/2"));
		assertThat(mappedUser2.meta.resourceType.toString(), is("User"));
		assertThat(mappedUser2.groups,
				hasItems(SCIMUserGroupResource.builder().withType(GroupType.direct.toString())
						.withValue("/scim/Members2").withDisplay("Members2")
						.withRef(URI.create("https://localhost:2443/scim/Group/%2Fscim%2FMembers2")).build()));

		assertThat(mappedUser2.userName, is("email2@email.com"));

	}
}
