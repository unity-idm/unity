/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.groups;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.renderers.HtmlRenderer;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Displays groups tree
 * 
 * @author P.Piernik
 *
 */
class GroupsTree extends TreeGrid<GroupNode>
{
	private TreeData<GroupNode> treeData;
	private GroupsController controller;
	private List<SingleActionHandler<GroupNode>> rowActionHandlers;
	private String projectPath;

	public GroupsTree(MessageSource msg, GroupsController controller,
			List<SingleActionHandler<GroupNode>> actions, String projectPath) throws ControllerException
	{
		this.controller = controller;
		this.rowActionHandlers = actions;
		this.projectPath = projectPath;

		treeData = new TreeData<>();
		TreeDataProvider<GroupNode> dataProvider = new TreeDataProvider<>(treeData);
		dataProvider.setSortComparator((g1, g2) -> g1.toString().compareTo(g2.toString()));
		setDataProvider(dataProvider);
		addColumn(n -> (n.htmlPrivacyIcon + n.htmlIcon + " " + n.toString()), new HtmlRenderer())
				.setCaption(msg.getMessage("DelegatedGroupsTree.group"));

		createActionColumn(msg.getMessage("DelegatedGroupsTree.action"));
		setPrimaryStyleName(Styles.vGroupBrowser.toString());
		
		setHeaderVisible(false);
		setRowHeight(34);
		setSizeFull();
		loadNode(projectPath, null);
		expand(treeData.getChildren(null));
		setWidth(100, Unit.PERCENTAGE);
	}

	private void createActionColumn(String caption)
	{
		addComponentColumn(t -> {
			HamburgerMenu<GroupNode> menu = new HamburgerMenu<>();
			HashSet<GroupNode> target = new HashSet<>();
			target.add(t);
			menu.setTarget(target);
			menu.addActionHandlers(rowActionHandlers);
			return menu;

		}).setCaption(caption).setWidth(80).setResizable(false).setSortable(false);
	}

	private void loadNode(String path, GroupNode parent) throws ControllerException
	{
		Map<String, List<DelegatedGroup>> groupTree;

		groupTree = controller.getGroupTree(projectPath, path);

		List<DelegatedGroup> rootGrs = groupTree.get(null);
		if (rootGrs == null)
			return;
		for (DelegatedGroup rootGr : rootGrs)
		{
			GroupNode rootNode = new GroupNode(rootGr, parent);
			treeData.addItem(parent, rootNode);
			addChilds(rootNode, groupTree);
		}
	}

	public void reloadNode(GroupNode node) throws ControllerException
	{
		treeData.removeItem(node);
		loadNode(node.getPath(), node.parent);
		getDataProvider().refreshAll();

	}

	public List<GroupNode> getChildren(GroupNode node)
	{
		return treeData.getChildren(node);
	}

	private void addChilds(GroupNode parentNode, Map<String, List<DelegatedGroup>> groupTree)
	{
		for (DelegatedGroup child : groupTree.get(parentNode.getPath()))
		{
			GroupNode childNode = new GroupNode(child, parentNode);
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

	public void expandRoot()
	{
		expand(treeData.getRootItems());
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
