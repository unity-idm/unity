/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.registration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.group.Group;

public class TestGroupPatternMatcher
{
	@Test
	public void shouldMatchWithSingleStar()
	{
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/*/users")).isTrue();
	}

	@Test
	public void shouldMatchWithSingleStarPartial()
	{
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/*o/users")).isTrue();
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/f*o/users")).isTrue();
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/g*/users")).isFalse();
	}

	@Test
	public void shouldMatchWithDoubleStarInMiddle()
	{
		assertThat(GroupPatternMatcher.matches("/group/foo/bar/users", "/group/**/users")).isTrue();
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/**/users")).isTrue();
		assertThat(GroupPatternMatcher.matches("/group/foo/bar/baz/users", "/group/**/users")).isTrue();

		assertThat(GroupPatternMatcher.matches("/group/foo/bar/baz", "/group/**/users")).isFalse();
	}

	@Test
	public void shouldMatchWithDoubleStarTrailing()
	{
		assertThat(GroupPatternMatcher.matches("/group/foo/bar/users", "/group/**")).isTrue();
		assertThat(GroupPatternMatcher.matches("/group/foo", "/group/**")).isTrue();
	}
	
	@Test
	public void shouldDisallowEmptyGroupElement()
	{
		assertThat(GroupPatternMatcher.matches("/group/users", "/group/*/users")).isFalse();
		assertThat(GroupPatternMatcher.matches("/group", "/group/**")).isTrue();
	}
	
	@Test
	public void shouldReturnFilteredByEnumeration()
	{
		List<Group> filtered = GroupPatternMatcher.filterMatching(Lists.newArrayList(new Group("/A"), new Group("/C"), new Group("/D")), 
				Lists.newArrayList("/A", "/B"));
		
		assertThat(filtered).contains(new Group("/A"));
		assertThat(filtered).hasSize(1);
	}
}
