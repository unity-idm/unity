/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

import static org.junit.Assert.assertThat;

import java.util.Set;

import org.junit.Test;

import io.codearte.catchexception.shade.mockito.internal.util.collections.Sets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.hasItem;

public class GroupTest
{
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
	public void shouldEstablishOnlyParents()
	{
		Group parent1 = new Group("/parent1");
		Group parent2 = new Group("/parent2");
		Group parent3 = new Group("/parent3");
		Group child1 = new Group("/parent1/child1");
		Group child2 = new Group("/parent1/child2");
		Group child3 = new Group("/parent2/child3");

		Set<Group> parents = Group.establishOnlyParentGroups(
				Sets.newSet(parent1, parent2, parent3, child1, child2, child3));

		assertThat(parents, hasItem(parent1));
		assertThat(parents, hasItem(parent2));
		assertThat(parents, hasItem(parent3));
		assertThat(parents, not(hasItem(child1)));
		assertThat(parents, not(hasItem(child2)));
		assertThat(parents, not(hasItem(child3)));
	}

	@Test
	public void shouldEstablishOnlyChilds()
	{
		Group parent1 = new Group("/parent1");
		Group parent2 = new Group("/parent2");
		Group parent3 = new Group("/parent3");
		Group child1 = new Group("/parent1/child1");
		Group child2 = new Group("/parent1/child2");
		Group child3 = new Group("/parent2/child3");
		Group child4 = new Group("/parent2/child3/child4");
		Group child5 = new Group("/parent2/child3/child5");

		Set<Group> childs = Group.establishOnlyChildGroups(
				Sets.newSet(parent1, parent2, parent3, child1, child2, child3, child4, child5));

		assertThat(childs, not(hasItem(parent1)));
		assertThat(childs, not(hasItem(parent2)));
		assertThat(childs, hasItem(parent3));
		assertThat(childs, hasItem(child1));
		assertThat(childs, hasItem(child2));
		assertThat(childs, hasItem(child4));
		assertThat(childs, hasItem(child5));
	}
}
