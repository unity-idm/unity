/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Group;

public class MembershipGroupsUtilsTest
{

	@Test
	public void shouldInlcudeWildcardGroups() throws EngineException
	{
		List<String> effectiveMembershipGroups = MembershipGroupsUtils.getEffectiveMembershipGroups(List.of("/B*"),
				Collections.emptyList(),
				Map.of("/", new Group("/"), "/A", new Group("/A"), "/B", new Group("/B"), "/B/C", new Group("/B/C")));

		assertThat(effectiveMembershipGroups.size(), is(2));
		assertThat(effectiveMembershipGroups, hasItems("/B", "/B/C"));
	}

	@Test
	public void shouldExcludeWildcardGroups() throws EngineException
	{

		List<String> effectiveMembershipGroups = MembershipGroupsUtils.getEffectiveMembershipGroups(List.of("/A/**"),
				List.of("/A/B*"),
				Map.of("/", new Group("/"), "/A", new Group("/A"), "/A/B", new Group("/A/B"), "/B", new Group("/B"),
						"/B/C", new Group("/B/C"), "/A/Bar", new Group("/A/Bar"), "/A/B/C", new Group("/A/B/C")));

		assertThat(effectiveMembershipGroups.size(), is(1));
		assertThat(effectiveMembershipGroups, hasItems("/A"));
	}

	@Test
	public void shouldExcludeGroupsWithChildren() throws EngineException
	{
		List<String> effectiveMembershipGroups = MembershipGroupsUtils.getEffectiveMembershipGroups(List.of("/A/**"),
				List.of("/A/B"),
				Map.of("/", new Group("/"), "/A", new Group("/A"), "/A/B", new Group("/A/B"), "/B", new Group("/B"),
						"/B/C", new Group("/B/C"), "/A/Bar", new Group("/A/Bar"), "/A/B/C", new Group("/A/B/C")));

		assertThat(effectiveMembershipGroups.size(), is(2));
		assertThat(effectiveMembershipGroups, hasItems("/A", "/A/Bar"));
	}

	@Test
	public void shouldIncludeRegularGroup() throws EngineException
	{

		List<String> effectiveMembershipGroups = MembershipGroupsUtils.getEffectiveMembershipGroups(List.of("/A"),
				Collections.emptyList(), Map.of("/", new Group("/"), "/A", new Group("/A"), "/B", new Group("/B")));

		assertThat(effectiveMembershipGroups.size(), is(1));
		assertThat(effectiveMembershipGroups, hasItems("/A"));
	}

	@Test
	public void shouldExcludeRegularGroup() throws EngineException
	{

		List<String> effectiveMembershipGroups = MembershipGroupsUtils.getEffectiveMembershipGroups(List.of("/**"),
				List.of("/A"), Map.of("/", new Group("/"), "/A", new Group("/A"), "/B", new Group("/B")));

		assertThat(effectiveMembershipGroups.size(), is(2));
		assertThat(effectiveMembershipGroups, hasItems("/", "/B"));
	}

	@Test
	public void shouldIncludeChildrenGroup() throws EngineException
	{

		List<String> effectiveMembershipGroups = MembershipGroupsUtils.getEffectiveMembershipGroups(List.of("/A"),
				Collections.emptyList(),
				Map.of("/", new Group("/"), "/A", new Group("/A"), "/A/C", new Group("/A/C"), "/B", new Group("/B")));

		assertThat(effectiveMembershipGroups.size(), is(2));
		assertThat(effectiveMembershipGroups, hasItems("/A", "/A/C"));
	}
}
