/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.engine.bulk.BulkQueryServiceImpl.GroupsTree;

class GroupsTreeTest
{
	@Test
	void shouldReturnDirectSubGroups()
	{
		GroupsTree tree = new GroupsTree(List.of(
				"/", 
				"/elementA", 
				"/elementB", 
				"/elementB/child1", 
				"/elementB/child1/leaf",
				"/elementB/child2", 
				"/elementB/child2/leaf"));

		assertThat(tree.getDirectSubGroups("/")).isEqualTo(List.of("/elementA", "/elementB"));
		assertThat(tree.getDirectSubGroups("/elementB")).isEqualTo(List.of("/elementB/child1", "/elementB/child2"));
		assertThat(tree.getDirectSubGroups("/elementB/child1")).isEqualTo(List.of("/elementB/child1/leaf"));
		assertThat(tree.getDirectSubGroups("/elementB/child1/leaf")).isEqualTo(List.of());
	}

	@Test
	void shouldReturnEmptyWhenNoGroup()
	{
		GroupsTree tree = new GroupsTree(List.of("/"));

		assertThat(tree.getDirectSubGroups("/__not_existsing_group__")).isEqualTo(List.of());
	}
}
