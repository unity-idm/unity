/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.hamcrest.CoreMatchers.hasItems;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.imunity.scim.config.SCIMEndpointConfiguration;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Group;

@RunWith(MockitoJUnitRunner.class)
public class MembershipGroupProviderTest
{
	private MembershipGroupsProvider provider;

	@Mock
	private GroupsManagement groupsManagement;

	@Test
	public void shouldInlcudeWildcardGroups() throws EngineException
	{

		provider = new MembershipGroupsProvider(groupsManagement);
		when(groupsManagement.getAllGroups()).thenReturn(
				Map.of("/", new Group("/"), "/A", new Group("/A"), "/B", new Group("/B"), "/B/C", new Group("/B/C")));

		List<String> effectiveMembershipGroups = provider.getEffectiveMembershipGroups(SCIMEndpointConfiguration
				.builder().withSchemas(Collections.emptyList()).withMembershipGroups(List.of("/B*")).build());

		assertThat(effectiveMembershipGroups.size(), is(2));
		assertThat(effectiveMembershipGroups, hasItems("/B", "/B/C"));
	}

	@Test
	public void shouldExcludeWildcardGroups() throws EngineException
	{

		provider = new MembershipGroupsProvider(groupsManagement);
		when(groupsManagement.getAllGroups())
				.thenReturn(Map.of("/", new Group("/"), "/A", new Group("/A"), "/B", new Group("/B"), "/B/C",
						new Group("/B/C"), "/A/Bar", new Group("/A/Bar"), "/A/B/C", new Group("/A/B/C")));

		List<String> effectiveMembershipGroups = provider
				.getEffectiveMembershipGroups(SCIMEndpointConfiguration.builder().withSchemas(Collections.emptyList())
						.withMembershipGroups(List.of("/A/**")).withExcludedMembershipGroups(List.of("/A/B*")).build());

		assertThat(effectiveMembershipGroups.size(), is(2));
		assertThat(effectiveMembershipGroups, hasItems("/A", "/A/B/C"));
	}

	@Test
	public void shouldIncludeRegularGroup() throws EngineException
	{

		provider = new MembershipGroupsProvider(groupsManagement);
		when(groupsManagement.getAllGroups())
				.thenReturn(Map.of("/", new Group("/"), "/A", new Group("/A"), "/B", new Group("/B")));

		List<String> effectiveMembershipGroups = provider.getEffectiveMembershipGroups(SCIMEndpointConfiguration
				.builder().withSchemas(Collections.emptyList()).withMembershipGroups(List.of("/A")).build());

		assertThat(effectiveMembershipGroups.size(), is(1));
		assertThat(effectiveMembershipGroups, hasItems("/A"));
	}

	@Test
	public void shouldExcludeRegularGroup() throws EngineException
	{

		provider = new MembershipGroupsProvider(groupsManagement);
		when(groupsManagement.getAllGroups())
				.thenReturn(Map.of("/", new Group("/"), "/A", new Group("/A"), "/B", new Group("/B")));

		List<String> effectiveMembershipGroups = provider
				.getEffectiveMembershipGroups(SCIMEndpointConfiguration.builder().withSchemas(Collections.emptyList())
						.withMembershipGroups(List.of("/**")).withExcludedMembershipGroups(List.of("/A")).build());

		assertThat(effectiveMembershipGroups.size(), is(2));
		assertThat(effectiveMembershipGroups, hasItems("/", "/B"));
	}

	@Test
	public void shouldIncludeChildrenGroup() throws EngineException
	{
		provider = new MembershipGroupsProvider(groupsManagement);
		when(groupsManagement.getAllGroups()).thenReturn(
				Map.of("/", new Group("/"), "/A", new Group("/A"), "/A/C", new Group("/A/C"), "/B", new Group("/B")));

		List<String> effectiveMembershipGroups = provider.getEffectiveMembershipGroups(SCIMEndpointConfiguration
				.builder().withSchemas(Collections.emptyList()).withMembershipGroups(List.of("/A")).build());

		assertThat(effectiveMembershipGroups.size(), is(2));
		assertThat(effectiveMembershipGroups, hasItems("/A", "/A/C"));
	}
}
