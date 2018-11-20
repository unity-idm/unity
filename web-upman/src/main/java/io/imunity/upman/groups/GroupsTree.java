/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.groups;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Sets;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.renderers.HtmlRenderer;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Displays groups tree
 * 
 * @author P.Piernik
 *
 */
public class GroupsTree extends TreeGrid<GroupNode>
{
	private TreeData<GroupNode> treeData;
	private GroupsController controller;
	private UnityMessageSource msg;
	private List<SingleActionHandler<GroupNode>> rowActionHandlers;
	private String root;

	public GroupsTree(UnityMessageSource msg, GroupsController controller,
			List<SingleActionHandler<GroupNode>> actions, String root)
			throws ControllerException
	{
		this.controller = controller;
		this.msg = msg;
		this.rowActionHandlers = actions;
		this.root = root;

		treeData = new TreeData<>();
		setDataProvider(new TreeDataProvider<>(treeData));

		addColumn(n -> n.getIcon() + " " + n.toString(), new HtmlRenderer())
				.setCaption(msg.getMessage("GroupTree.group"));

		addComponentColumn(g -> {
			HamburgerMenu<GroupNode> menu = new HamburgerMenu<GroupNode>();
			menu.setTarget(Sets.newHashSet(g));
			menu.addActionHandlers(rowActionHandlers);
			return menu;

		}).setCaption(msg.getMessage("GroupTree.action")).setWidth(80).setResizable(false);

		loadNode(root, null);
		expand(treeData.getChildren(null));
		setWidth(100, Unit.PERCENTAGE);
	}

	private void loadNode(String path, GroupNode parent) throws ControllerException
	{
		Map<String, List<Group>> groupTree;

		groupTree = controller.getGroupTree(root, path);

		for (Group rootGr : groupTree.get(null))
		{
			GroupNode rootNode = new GroupNode(msg, rootGr, parent);
			treeData.addItem(parent, rootNode);
			addChilds(rootNode, groupTree);

		}

	}

	public void reloadNode(GroupNode node) throws ControllerException
	{
		treeData.removeItem(node);
		loadNode(node.getPath(), node.getParentNode());
		getDataProvider().refreshAll();

	}

	private void addChilds(GroupNode parentNode, Map<String, List<Group>> groupTree)
	{
		for (Group child : groupTree.get(parentNode.getPath()))
		{
			GroupNode childNode = new GroupNode(msg, child, parentNode);
			treeData.addItem(parentNode, childNode);
			addChilds(childNode, groupTree);
		}

	}

	private void expandItemsRecursively(Collection<GroupNode> items)
	{
		for (GroupNode node : items)
		{
			expand(node);
			for (GroupNode child : treeData.getChildren(node))
				expandItemsRecursively(Arrays.asList(child));
		}
	}

	public void expandAll()
	{
		expandItemsRecursively(treeData.getRootItems());

	}

	private void collapseItemsRecursively(Collection<GroupNode> items)
	{
		for (GroupNode node : items)
		{
			collapse(node);
			for (GroupNode child : treeData.getChildren(node))
				collapseItemsRecursively(Arrays.asList(child));
		}
	}

	public void collapseAll()
	{
		collapseItemsRecursively(treeData.getChildren(null));
	}

}
