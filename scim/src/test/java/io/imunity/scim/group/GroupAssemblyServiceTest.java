/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.imunity.scim.common.ListResponse;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.group.GroupMember.MemberType;

@RunWith(MockitoJUnitRunner.class)
public class GroupAssemblyServiceTest
{
	private GroupAssemblyService groupAssemblyService;

	@Before
	public void init()
	{
		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https://localhost:2443/scim"),
				"/scim", List.of("/scim/Members1", "/scim/Members2"), Collections.emptyList());
		groupAssemblyService = new GroupAssemblyService(configuration);
	}

	@Test
	public void shouldAssemblySingleGroupWithMembers() throws MalformedURLException
	{
		GroupData groupData = GroupData.builder().withDisplayName("name1").withId("/scim/id")
				.withMembers(List.of(
						GroupMember.builder().withDisplayName("Memeber1").withType(MemberType.User).withValue("1")
								.build(),
						GroupMember.builder().withDisplayName("Group1").withType(MemberType.Group)
								.withValue("/scim/id/sub").build()

				)).build();

		SCIMGroupResource mappedGroup = groupAssemblyService.mapToGroupResource(groupData);

		assertThat(mappedGroup.meta.resourceType.toString(), is("Group"));
		assertThat(mappedGroup.meta.location.toURL().toExternalForm(),
				is("https://localhost:2443/scim/Group/%2Fscim%2Fid"));
		assertThat(mappedGroup.id, is("/scim/id"));
		assertThat(mappedGroup.members,
				hasItems(
						SCIMGroupMemberResource.builder().withDisplay("Memeber1").withType(MemberType.User.toString())
								.withValue("1").withRef(URI.create("https://localhost:2443/scim/User/1")).build(),
						SCIMGroupMemberResource.builder().withDisplay("Group1").withType(MemberType.Group.toString())
								.withValue("/scim/id/sub")
								.withRef(URI.create("https://localhost:2443/scim/Group/%2Fscim%2Fid%2Fsub")).build()));
	}

	@Test
	public void shouldAssemblyGroupsWithMixedTypeMembers() throws MalformedURLException
	{

		GroupData groupData1 = GroupData.builder().withDisplayName("name1").withId("/scim/id1")
				.withMembers(List.of(
						GroupMember.builder().withDisplayName("Memeber1").withType(MemberType.User).withValue("1")
								.build(),
						GroupMember.builder().withDisplayName("Group1").withType(MemberType.Group)
								.withValue("/scim/id1/sub").build()

				)).build();

		GroupData groupData2 = GroupData.builder().withDisplayName("name2").withId("/scim/id2").build();

		ListResponse<SCIMGroupResource> mappedGroups = groupAssemblyService
				.mapToGroupsResource(List.of(groupData1, groupData2));
		assertThat(mappedGroups.totalResults, is(2));
		SCIMGroupResource mappedGroup = mappedGroups.resources.stream().filter(g -> g.id.equals("/scim/id1")).findAny()
				.get();
		assertThat(mappedGroup.meta.location.toURL().toExternalForm(),
				is("https://localhost:2443/scim/Group/%2Fscim%2Fid1"));
		assertThat(mappedGroup.id, is("/scim/id1"));
		assertThat(mappedGroup.members,
				hasItems(
						SCIMGroupMemberResource.builder().withDisplay("Memeber1").withType(MemberType.User.toString())
								.withValue("1").withRef(URI.create("https://localhost:2443/scim/User/1")).build(),
						SCIMGroupMemberResource.builder().withDisplay("Group1").withType(MemberType.Group.toString())
								.withValue("/scim/id1/sub")
								.withRef(URI.create("https://localhost:2443/scim/Group/%2Fscim%2Fid1%2Fsub")).build()));

		mappedGroup = mappedGroups.resources.stream().filter(g -> g.id.equals("/scim/id2")).findAny().get();
		assertThat(mappedGroup.meta.location.toURL().toExternalForm(),
				is("https://localhost:2443/scim/Group/%2Fscim%2Fid2"));
		assertThat(mappedGroup.id, is("/scim/id2"));
		assertThat(mappedGroup.members.size(), is(0));

	}
	
	@Test
	public void shouldAssemblyEmptyGroup() throws MalformedURLException
	{
		GroupData groupData = GroupData.builder().withDisplayName("name1").withId("/scim/id")
				.build();
		SCIMGroupResource mappedGroup = groupAssemblyService.mapToGroupResource(groupData);

		assertThat(mappedGroup.meta.resourceType.toString(), is("Group"));
		assertThat(mappedGroup.meta.location.toURL().toExternalForm(),
				is("https://localhost:2443/scim/Group/%2Fscim%2Fid"));
		assertThat(mappedGroup.id, is("/scim/id"));
		assertThat(mappedGroup.members.size(), is(0));
	}
}
