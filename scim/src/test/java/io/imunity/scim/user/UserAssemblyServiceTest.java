/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.imunity.scim.common.ListResponse;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.config.SchemaType;
import io.imunity.scim.config.SchemaWithMapping;
import io.imunity.scim.user.mapping.evaluation.UserSchemaEvaluator;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;

@ExtendWith(MockitoExtension.class)
public class UserAssemblyServiceTest
{

	private UserAssemblyService assemblyService;

	@Mock
	private UserSchemaEvaluator userSchemaEvaluator;
	@Mock
	private GroupsManagement groupsManagement;
	@Mock
	private UserAuthzService authzService;

	
	@BeforeEach
	public void init()
	{
		SCIMEndpointDescription configuration = SCIMEndpointDescription.builder()
				.withBaseLocation(URI.create("https://localhost:2443/scim")).withRootGroup("/scim")
				.withMembershipGroups(List.of("/scim/Members1", "/scim/Members2"))
				.withSchemas(List.of(SchemaWithMapping.builder().withType(SchemaType.USER_CORE).withName("UserCore").withId("UC")
						.withEnable(true).build(),
						SchemaWithMapping.builder().withType(SchemaType.USER).withName("UserExt").withId("UE")
								.withEnable(true).build()))
				.build();
		when(authzService.getFilter()).thenReturn(s -> true);
		assemblyService = new UserAssemblyService(configuration, userSchemaEvaluator, groupsManagement, authzService);
	}

	@Test
	public void shouldAssemblySingleUserAndEvalEnabledSchemas() throws MalformedURLException, EngineException
	{
		User user = User.builder().withEntityId(0L)
				.withGroups(Set.of(new Group("/scim/Members1"), new Group("/scim/Members2")))
				.withIdentities(List.of(new Identity(PersistentIdentity.ID, "0", 0l, "0"))).build();

		SCIMUserResource userRes = assemblyService.mapToUserResource(user);

		verify(userSchemaEvaluator).evalUserSchema(eq(user),
				eq(List.of(SchemaWithMapping.builder().withType(SchemaType.USER_CORE).withName("UserCore").withId("UC")
						.withEnable(true).build(), SchemaWithMapping.builder().withType(SchemaType.USER).withName("UserExt").withId("UE")
						.withEnable(true).build())),
				any());

		assertThat(userRes.id).isEqualTo("0");
		assertThat(userRes.schemas).contains("UC", "UE");
		assertThat(userRes.meta.location.toURL().toExternalForm()).isEqualTo("https://localhost:2443/scim/Users/0");
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
				eq(List.of(SchemaWithMapping.builder().withType(SchemaType.USER_CORE).withName("UserCore").withId("UC")
						.withEnable(true).build(),SchemaWithMapping.builder().withType(SchemaType.USER).withName("UserExt").withId("UE")
						.withEnable(true).build())),
				any());

		verify(userSchemaEvaluator).evalUserSchema(eq(user2),
				eq(List.of(SchemaWithMapping.builder().withType(SchemaType.USER_CORE).withName("UserCore").withId("UC")
						.withEnable(true).build(), SchemaWithMapping.builder().withType(SchemaType.USER).withName("UserExt").withId("UE")
						.withEnable(true).build())),
				any());
		assertThat(userRes.totalResults).isEqualTo(2);
		assertThat(userRes.resources.stream().map(r -> r.id).collect(Collectors.toList())).contains("1", "2");
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
		assertThat(mappedUser.meta.created).isEqualTo(creation.toInstant());
		assertThat(mappedUser.meta.lastModified).isEqualTo(update.toInstant());

	}
}
