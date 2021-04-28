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

public class GroupTest {
	@Test
	public void shouldCheckIsChildGroup() {
		Group parent = new Group("/parent");
		Group child = new Group("/parent/child");

		assertThat(child.isChild(parent), is(true));
	}

	@Test
	public void shouldCheckIsChildNotSameGroup() {
		Group parent = new Group("/parent");
		Group child = new Group("/parent/child");

		assertThat(child.isChildNotSame(parent), is(true));
	}

	@Test
	public void shouldReturnOnlyRootsOfGroups() {
		Group parent1 = new Group("/parent1");
		Group parent2 = new Group("/parent2");
		Group child1 = new Group("/parent1/child1");

		Set<Group> parents = Group.getRootsOfSet(Sets.newSet(parent1, parent2, child1));
		assertThat(parents, hasItem(parent1));
		assertThat(parents, hasItem(parent2));
		assertThat(parents, not(hasItem(child1)));
	}

	@Test
	public void shouldNotReturnDirectChild() {
		Group parent1 = new Group("/parent1");
		Group child1 = new Group("/parent1/child1");
		Group child2 = new Group("/parent1/child2");

		Set<Group> parents = Group.getRootsOfSet(Sets.newSet(parent1, child1, child2));
		assertThat(parents, hasItem(parent1));
		assertThat(parents, not(hasItem(child1)));
		assertThat(parents, not(hasItem(child2)));
	}

	@Test
	public void shouldNotInclude2ndLevelChildGroup() {
		Group parent1 = new Group("/parent1");
		Group child1 = new Group("/parent1/child1");
		Group child2 = new Group("/parent1/child1/child2");

		Set<Group> parents = Group.getRootsOfSet(Sets.newSet(parent1, child1, child2));
		assertThat(parents, hasItem(parent1));
		assertThat(parents, not(hasItem(child1)));
		assertThat(parents, not(hasItem(child2)));
	}
}
