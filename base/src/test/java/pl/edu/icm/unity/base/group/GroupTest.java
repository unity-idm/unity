/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;

@ExtendWith(MockitoExtension.class)
public class GroupTest
{

	@Mock
	private MessageSource msg;

	@Test
	public void shouldCheckIsChildGroup()
	{
		Group parent = new Group("/parent");
		Group child = new Group("/parent/child");

		assertThat(child.isChild(parent)).isEqualTo(true);
	}

	@Test
	public void shouldCheckIsChildNotSameGroup()
	{
		Group parent = new Group("/parent");
		Group child = new Group("/parent/child");

		assertThat(child.isChildNotSame(parent)).isEqualTo(true);
	}

	@Test
	public void shouldReturnOnlyRootsOfGroups()
	{
		Group parent1 = new Group("/parent1");
		Group parent2 = new Group("/parent2");
		Group child1 = new Group("/parent1/child1");

		Set<Group> parents = Group.getRootsOfSet(Set.of(parent1, parent2, child1));
		assertThat(parents).contains(parent1);
		assertThat(parents).contains(parent2);
		assertThat(parents).doesNotContain(child1);
	}

	@Test
	public void shouldNotReturnDirectChild()
	{
		Group parent1 = new Group("/parent1");
		Group child1 = new Group("/parent1/child1");
		Group child2 = new Group("/parent1/child2");

		Set<Group> parents = Group.getRootsOfSet(Set.of(parent1, child1, child2));
		assertThat(parents).contains(parent1);
		assertThat(parents).doesNotContain(child1);
		assertThat(parents).doesNotContain(child2);
	}

	@Test
	public void shouldNotInclude2ndLevelChildGroup()
	{
		Group parent1 = new Group("/parent1");
		Group child1 = new Group("/parent1/child1");
		Group child2 = new Group("/parent1/child1/child2");

		Set<Group> parents = Group.getRootsOfSet(Set.of(parent1, child1, child2));
		assertThat(parents).contains(parent1);
		assertThat(parents).doesNotContain(child1);
		assertThat(parents).doesNotContain(child2);
	}
	
	@Test
	public void shouldReturnOnlyChildren()
	{
		Group parent1 = new Group("/parent1");
		Group child1 = new Group("/parent1/child1");
		Group child2 = new Group("/parent1/child2");

		Set<Group> childs = Group.getOnlyChildrenOfSet(
				Set.of(parent1, child1, child2));

		assertThat(childs).doesNotContain(parent1);
		assertThat(childs).contains(child1);
		assertThat(childs).contains(child2);
	}
	
	@Test
	public void shouldReturnOnlyLastChildren()
	{
		Group parent1 = new Group("/parent1");
		Group child1 = new Group("/parent1/child1");
		Group child2 = new Group("/parent1/child1/child2");

		Set<Group> childs = Group.getOnlyChildrenOfSet(
				Set.of(parent1, child1, child2));

		assertThat(childs).doesNotContain(parent1);
		assertThat(childs).doesNotContain(child1);
		assertThat(childs).contains(child2);	
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

		assertThat(group.getDisplayedNameShort(msg)).isEqualTo(displayedName);
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

		assertThat(group.getDisplayedNameShort(msg)).isEqualTo(displayedName);
	}
	
	@Test
	public void shouldReturnGroupLastPathElementWhenGetShortDisplayedName()
	{
		Group group = new Group("/parent");
		I18nString displayedName = new I18nString("/parent");
		group.setDisplayedName(displayedName);
		
		when(msg.getDefaultLocaleCode()).thenReturn("en");
		when(msg.getLocaleCode()).thenReturn("en");

		assertThat(group.getDisplayedNameShort(msg)).isEqualTo(new I18nString("parent"));
	}
	
	@Test
	public void shouldReturnDefaultValueOfDisplayedNameWhenGetShortDisplayedName()
	{
		Group group = new Group("/parent");
		I18nString displayedName = new I18nString("DEFAULT");
		group.setDisplayedName(displayedName);
		
		when(msg.getDefaultLocaleCode()).thenReturn("en");
		when(msg.getLocaleCode()).thenReturn("en");

		assertThat(group.getDisplayedNameShort(msg)).isEqualTo(displayedName);
	}
}