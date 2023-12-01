/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.imunity.upman.front.model.Group;
import io.imunity.upman.front.model.GroupTreeNode;
import pl.edu.icm.unity.base.i18n.I18nString;

public class TestGroupTreeNode
{
	private GroupTreeNode root;

	@BeforeEach
	public void setUp()
	{
		Group group = new Group("/", new I18nString("group"), "group", false, false, "", false, 0);
		root = new GroupTreeNode(group, 0);
	}

	@Test
	public void shouldGetAllNodes()
	{
		Group groupA = new Group("/A", new I18nString("group"), "groupA", false, false, "", false, 0);
		Group groupB = new Group("/B", new I18nString("group"), "groupB", false, false, "", false, 0);
		Group groupAB = new Group("/A/B", new I18nString("group"), "sameName", false, false, "", false, 0);
		Group groupAC = new Group("/A/C", new I18nString("group"), "sameName", false, false, "", false, 0);
		Group groupBA = new Group("/B/A", new I18nString("group"), "groupBA", false, false, "", false, 0);
		Group groupBD = new Group("/B/D", new I18nString("group"), "groupBD", false, false, "", false, 0);

		root.addChildren(groupA, groupB, groupAC, groupAB, groupBD, groupBA);

		List<Group> allNodes = root.getNodeWithAllOffspring().stream()
				.map(node -> node.group)
				.collect(Collectors.toList());

		assertThat(allNodes).isEqualTo(List.of(root.group, groupA, groupAB, groupAC, groupB, groupBA, groupBD));
	}

	@Test
	public void shouldGetAllOffsprings()
	{
		Group groupA = new Group("/A", new I18nString("group"), "groupA", false, false, "", false, 0);
		Group groupB = new Group("/B", new I18nString("group"), "groupB", false, false, "", false, 0);
		Group groupAB = new Group("/A/B", new I18nString("group"), "groupAB", false, false, "", false, 0);
		Group groupBA = new Group("/B/A", new I18nString("group"), "groupBA", false, false, "", false, 0);

		root.addChildren(groupA, groupB, groupAB, groupBA);

		List<Group> allNodes = root.getAllOffspring().stream()
				.map(node -> node.group)
				.collect(Collectors.toList());

		assertThat(allNodes).isEqualTo(List.of(groupA, groupAB, groupB, groupBA));
	}

	@Test
	public void shouldGetAllParents()
	{
		Group groupA = new Group("/A", new I18nString("group"), "groupA", false, false, "", false, 0);
		Group groupB = new Group("/B", new I18nString("group"), "groupB", false, false, "", false, 0);
		Group groupAB = new Group("/A/B", new I18nString("group"), "groupAB", false, false, "", false, 0);
		Group groupAC = new Group("/A/C", new I18nString("group"), "groupAC", false, false, "", false, 0);
		Group groupBA = new Group("/B/A", new I18nString("group"), "groupBA", false, false, "", false, 0);

		root.addChildren(groupA, groupB, groupAB, groupAC, groupBA);

		GroupTreeNode firstGeneration = root.getChildren().iterator().next();
		GroupTreeNode secondGeneration = firstGeneration.getChildren().iterator().next();

		List<Group> ancestors = secondGeneration.getAllAncestors().stream()
				.map(node -> node.group)
				.collect(Collectors.toList());

		assertThat(ancestors).isEqualTo(List.of(groupA, root.group));
	}
}
