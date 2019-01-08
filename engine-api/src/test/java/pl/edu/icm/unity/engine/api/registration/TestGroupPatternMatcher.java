/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.registration;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.types.basic.Group;

public class TestGroupPatternMatcher
{
	@Test
	public void shouldMatchWithSingleStar()
	{
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/*/users"), is(true));
	}

	@Test
	public void shouldMatchWithSingleStarPartial()
	{
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/*o/users"), is(true));
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/f*o/users"), is(true));
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/g*/users"), is(false));
	}

	@Test
	public void shouldMatchWithDoubleStarInMiddle()
	{
		assertThat(GroupPatternMatcher.matches("/group/foo/bar/users", "/group/**/users"), is(true));
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/**/users"), is(true));
		assertThat(GroupPatternMatcher.matches("/group/foo/bar/baz/users", "/group/**/users"), is(true));

		assertThat(GroupPatternMatcher.matches("/group/foo/bar/baz", "/group/**/users"), is(false));
	}

	@Test
	public void shouldMatchWithDoubleStarTrailing()
	{
		assertThat(GroupPatternMatcher.matches("/group/foo/bar/users", "/group/**"), is(true));
		assertThat(GroupPatternMatcher.matches("/group/foo", "/group/**"), is(true));
	}
	
	@Test
	public void shouldDisallowEmptyGroupElement()
	{
		assertThat(GroupPatternMatcher.matches("/group/users", "/group/*/users"), is(false));
		assertThat(GroupPatternMatcher.matches("/group", "/group/**"), is(true));
	}
	
	@Test
	public void shouldReturnFilteredByEnumeration()
	{
		List<Group> filtered = GroupPatternMatcher.filterMatching(Lists.newArrayList(new Group("/A"), new Group("/C"), new Group("/D")), 
				Lists.newArrayList("/A", "/B"));
		
		assertThat(filtered, hasItems(new Group("/A")));
		assertThat(filtered.size(), is(1));
	}
}
