/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.engine.bulk.BulkQueryServiceImpl.GroupsTree;

class GroupsTreeTest
{
	@Test
	void shouldReturnDirectSubGroups()
	{
		GroupsTree tree = new GroupsTree(List.of(
				new Group("/"), 
				new Group("/elementA"), 
				new Group("/elementB"), 
				new Group("/elementB/child1"), 
				new Group("/elementB/child1/leaf"),
				new Group("/elementB/child2"), 
				new Group("/elementB/child2/leaf")));

		assertThat(tree.getDirectSubGroups("/")).isEqualTo(List.of("/elementA", "/elementB"));
		assertThat(tree.getDirectSubGroups("/elementB")).isEqualTo(List.of("/elementB/child1", "/elementB/child2"));
		assertThat(tree.getDirectSubGroups("/elementB/child1")).isEqualTo(List.of("/elementB/child1/leaf"));
		assertThat(tree.getDirectSubGroups("/elementB/child1/leaf")).isEqualTo(List.of());
	}

	@Test
	void shouldReturnEmptyWhenNoGroup()
	{
		GroupsTree tree = new GroupsTree(List.of(new Group("/")));

		assertThat(tree.getDirectSubGroups("/__not_existsing_group__")).isEqualTo(List.of());
	}
}
