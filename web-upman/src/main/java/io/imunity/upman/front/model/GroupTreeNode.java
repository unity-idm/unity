/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.model;

import java.util.*;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.message.MessageSource;

public class GroupTreeNode
{
	private static final Comparator<GroupTreeNode> groupComparator = (GroupTreeNode node1, GroupTreeNode node2) -> {
		int result = node1.getDisplayedName().compareTo(node2.getDisplayedName());
		if(areDisplayedNamesEquals(result))
			return node1.getPath().compareTo(node2.getPath());
		return result;
	};

	private static boolean areDisplayedNamesEquals(int result)
	{
		return result == 0;
	}

	public final Group group;
	public final Optional<GroupTreeNode> parent;
	private final TreeSet<GroupTreeNode> children;
	private final int level;

	private GroupTreeNode(GroupTreeNode parent, Group group, int level)
	{
		this.parent = Optional.of(parent);
		this.group = group;
		this.level = level;
		this.children = new TreeSet<>(groupComparator);
	}

	public GroupTreeNode(Group group, int level)
	{
		this.parent = Optional.empty();
		this.group = group;
		this.level = level;
		this.children = new TreeSet<>(groupComparator);
	}

	public void addChildren(Group... groups)
	{
		for (Group group : groups)
			addChild(group);
	}

	public boolean addChild(Group group)
	{
		if(group.path.equals(this.group.path))
			return false;
		if(!group.path.startsWith(this.group.path))
			return false;
		for (GroupTreeNode child : children)
		{
			if(child.addChild(group))
				return true;
		}
		children.add(new GroupTreeNode(this, group, level + 1));
		return true;
	}

	public String getPath()
	{
		return group.path;
	}

	public boolean isRoot()
	{
		return parent.isEmpty();
	}

	public int getLevel()
	{
		return level;
	}

	public String getDisplayedName()
	{
		return group.currentDisplayedName;
	}

	public String getDisplayedNameWithDescription(MessageSource msg)
	{
		String description = "";
		if(isDelegationEnabled())
			description = " (" + msg.getMessage("Sub-project") + ")";
		if(isRoot())
			description = " (" + msg.getMessage("AllMemebers") + ")";
		return group.currentDisplayedName + description;
	}

	public List<GroupTreeNode> getChildren()
	{
		return List.copyOf(children);
	}

	public boolean isPublic()
	{
		return group.isPublic;
	}

	public boolean isDelegationEnabled()
	{
		return group.delegationEnabled;
	}

	public boolean isSubprojectsDelegationEnabled()
	{
		return group.subprojectsDelegationEnabled;
	}

	public List<GroupTreeNode> getNodeWithAllOffspring()
	{
		List<GroupTreeNode> groups = new LinkedList<>();
		groups.add(this);
		children.forEach(groupTreeNode -> groups.addAll(groupTreeNode.getNodeWithAllOffspring()));
		return groups;
	}

	public List<GroupTreeNode> getAllOffspring()
	{
		return cutOffParent().stream()
				.flatMap(groupTreeNode -> groupTreeNode.getNodeWithAllOffspring().stream())
				.collect(Collectors.toList());
	}

	public List<GroupTreeNode> getAllAncestors()
	{
		List<GroupTreeNode> parents = new LinkedList<>();
		Optional<GroupTreeNode> node = parent;
		while (node.isPresent())
		{
			parents.add(node.get());
			node = node.get().parent;
		}
		return parents;
	}

	private List<GroupTreeNode> cutOffParent()
	{
		return children.stream()
				.map(GroupTreeNode::copyTree)
				.collect(Collectors.toList());
	}

	private GroupTreeNode copyTree()
	{
		GroupTreeNode node = new GroupTreeNode(group, 0);
		children.forEach(child -> node.addChild(child.copyTree(node)));
		return node;
	}

	private GroupTreeNode copyTree(GroupTreeNode parent)
	{
		GroupTreeNode node = new GroupTreeNode(parent, group, parent.level + 1);
		children.forEach(child -> node.addChild(child.copyTree(node)));
		return node;
	}

	private void addChild(GroupTreeNode node)
	{
		children.add(node);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GroupTreeNode that = (GroupTreeNode) o;
		return level == that.level && Objects.equals(group, that.group) && Objects.equals(children, that.children);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(group, children, level);
	}

	@Override
	public String toString()
	{
		return "GroupTreeNode{" +
				"group=" + group +
				", children=" + children +
				", level=" + level +
				'}';
	}
}
