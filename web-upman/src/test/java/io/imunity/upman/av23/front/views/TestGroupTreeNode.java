/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views;

import io.imunity.upman.av23.front.model.Group;
import io.imunity.upman.av23.front.model.GroupTreeNode;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class TestGroupTreeNode
{
	private GroupTreeNode root;

	@Before
	public void setUp()
	{
		Group group = new Group("/", "group", false, false, "", false, 0);
		root = new GroupTreeNode(group, 0);
	}

	@Test
	public void shouldGetAllNodes()
	{
		Group groupA = new Group("/A", "groupA", false, false, "", false, 0);
		Group groupB = new Group("/B", "groupB", false, false, "", false, 0);
		Group groupAB = new Group("/A/B", "sameName", false, false, "", false, 0);
		Group groupAC = new Group("/A/C", "sameName", false, false, "", false, 0);
		Group groupBA = new Group("/B/A", "groupBA", false, false, "", false, 0);
		Group groupBD = new Group("/B/D", "groupBD", false, false, "", false, 0);

		root.addChild(groupA);
		root.addChild(groupB);
		root.addChild(groupAB);
		root.addChild(groupAC);
		root.addChild(groupBD);
		root.addChild(groupBA);

		List<Group> allNodes = root.getAllNodes().stream()
				.map(node -> node.group)
				.collect(Collectors.toList());

		assertEquals(List.of(root.group, groupA, groupAB, groupAC, groupB, groupBA, groupBD), allNodes);
	}

	@Test
	public void shouldGetAllChildrenElementsWithParentCutOff()
	{
		Group groupA = new Group("/A", "groupA", false, false, "", false, 0);
		Group groupB = new Group("/B", "groupB", false, false, "", false, 0);
		Group groupAB = new Group("/A/B", "groupAB", false, false, "", false, 0);
		Group groupAC = new Group("/A/C", "groupAC", false, false, "", false, 0);
		Group groupBA = new Group("/B/A", "groupBA", false, false, "", false, 0);
		Group groupBD = new Group("/B/D", "groupBD", false, false, "", false, 0);

		root.addChild(groupA);
		root.addChild(groupB);
		root.addChild(groupAB);
		root.addChild(groupAC);
		root.addChild(groupBD);
		root.addChild(groupBA);

		List<Group> allNodes = root.getAllChildrenElementsWithCutOff().stream()
				.map(node -> node.group)
				.collect(Collectors.toList());

		assertEquals(List.of(groupA, groupAB, groupAC, groupB, groupBA, groupBD), allNodes);
	}

	@Test
	public void shouldGetAllParents()
	{
		Group groupA = new Group("/A", "groupA", false, false, "", false, 0);
		Group groupB = new Group("/B", "groupB", false, false, "", false, 0);
		Group groupAB = new Group("/A/B", "groupAB", false, false, "", false, 0);
		Group groupAC = new Group("/A/C", "groupAC", false, false, "", false, 0);
		Group groupBA = new Group("/B/A", "groupBA", false, false, "", false, 0);
		Group groupBD = new Group("/B/D", "groupBD", false, false, "", false, 0);

		root.addChild(groupA);
		root.addChild(groupB);
		root.addChild(groupAB);
		root.addChild(groupAC);
		root.addChild(groupBD);
		root.addChild(groupBA);

		List<Group> allNodes = root.getChildren().iterator().next().getChildren().iterator().next().getAllParentsElements().stream()
				.map(node -> node.group)
				.collect(Collectors.toList());

		assertEquals(List.of(groupA, root.group), allNodes);
	}
}
