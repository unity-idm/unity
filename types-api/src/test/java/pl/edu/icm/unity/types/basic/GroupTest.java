/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.codearte.catchexception.shade.mockito.internal.util.collections.Sets;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.I18nString;

@RunWith(MockitoJUnitRunner.class)
public class GroupTest
{

	@Mock
	private MessageSource msg;

	@Test
	public void shouldCheckIsChildGroup()
	{
		Group parent = new Group("/parent");
		Group child = new Group("/parent/child");

		assertThat(child.isChild(parent), is(true));
	}

	@Test
	public void shouldCheckIsChildNotSameGroup()
	{
		Group parent = new Group("/parent");
		Group child = new Group("/parent/child");

		assertThat(child.isChildNotSame(parent), is(true));
	}

	@Test
	public void shouldReturnOnlyRootsOfGroups()
	{
		Group parent1 = new Group("/parent1");
		Group parent2 = new Group("/parent2");
		Group child1 = new Group("/parent1/child1");

		Set<Group> parents = Group.getRootsOfSet(Sets.newSet(parent1, parent2, child1));
		assertThat(parents, hasItem(parent1));
		assertThat(parents, hasItem(parent2));
		assertThat(parents, not(hasItem(child1)));
	}

	@Test
	public void shouldNotReturnDirectChild()
	{
		Group parent1 = new Group("/parent1");
		Group child1 = new Group("/parent1/child1");
		Group child2 = new Group("/parent1/child2");

		Set<Group> parents = Group.getRootsOfSet(Sets.newSet(parent1, child1, child2));
		assertThat(parents, hasItem(parent1));
		assertThat(parents, not(hasItem(child1)));
		assertThat(parents, not(hasItem(child2)));
	}

	@Test
	public void shouldNotInclude2ndLevelChildGroup()
	{
		Group parent1 = new Group("/parent1");
		Group child1 = new Group("/parent1/child1");
		Group child2 = new Group("/parent1/child1/child2");

		Set<Group> parents = Group.getRootsOfSet(Sets.newSet(parent1, child1, child2));
		assertThat(parents, hasItem(parent1));
		assertThat(parents, not(hasItem(child1)));
		assertThat(parents, not(hasItem(child2)));
	}

	@Test
	public void shouldReturnFullDisplayedNameWhenGetShortDisplayedName()
	{
		Group group = new Group("/parent");
		I18nString displayedName = new I18nString("/parent");
		displayedName.addValue("en", "GroupEN");
		group.setDisplayedName(displayedName);
		
		when(msg.getDefaultLocaleCode()).thenReturn("en");
		when(msg.getLocaleCode()).thenReturn("en");

		assertThat(group.getDisplayedNameShort(msg), is(displayedName));
	}
	
	@Test
	public void shouldReturnFullDisplayedNameDefaultLocaleWhenGetShortDisplayedName()
	{
		Group group = new Group("/parent");
		I18nString displayedName = new I18nString("/parent");
		displayedName.addValue("en", "GroupEN");
		group.setDisplayedName(displayedName);
		
		when(msg.getDefaultLocaleCode()).thenReturn("en");
		when(msg.getLocaleCode()).thenReturn("de");

		assertThat(group.getDisplayedNameShort(msg), is(displayedName));
	}
	
	@Test
	public void shouldReturnGroupLastPathElementWhenGetShortDisplayedName()
	{
		Group group = new Group("/parent");
		I18nString displayedName = new I18nString("/parent");
		group.setDisplayedName(displayedName);
		
		when(msg.getDefaultLocaleCode()).thenReturn("en");
		when(msg.getLocaleCode()).thenReturn("en");

		assertThat(group.getDisplayedNameShort(msg), is(new I18nString("parent")));
	}
	
	@Test
	public void shouldReturnDefaultValueOfDisplayedNameWhenGetShortDisplayedName()
	{
		Group group = new Group("/parent");
		I18nString displayedName = new I18nString("DEFAULT");
		group.setDisplayedName(displayedName);
		
		when(msg.getDefaultLocaleCode()).thenReturn("en");
		when(msg.getLocaleCode()).thenReturn("en");

		assertThat(group.getDisplayedNameShort(msg), is(displayedName));
	}

	@Test
	public void shouldReturnOnlyChildren()
	{
		Group parent1 = new Group("/parent1");
		Group child1 = new Group("/parent1/child1");
		Group child2 = new Group("/parent1/child2");

		Set<Group> childs = Group.getOnlyChildrenOfSet(
				Sets.newSet(parent1, child1, child2));

		assertThat(childs, not(hasItem(parent1)));
		assertThat(childs, hasItem(child1));
		assertThat(childs, hasItem(child2));	
	}
	
	@Test
	public void shouldReturnOnlyLastChildren()
	{
		Group parent1 = new Group("/parent1");
		Group child1 = new Group("/parent1/child1");
		Group child2 = new Group("/parent1/child1/child2");

		Set<Group> childs = Group.getOnlyChildrenOfSet(
				Sets.newSet(parent1, child1, child2));

		assertThat(childs, not(hasItem(parent1)));
		assertThat(childs, not(hasItem(child1)));
		assertThat(childs, hasItem(child2));	
	}
}
