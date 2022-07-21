/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.model;

import java.util.*;
import java.util.stream.Collectors;

public class GroupTreeNode
{
	public final Group group;
	public final Optional<GroupTreeNode> parent;
	private final PriorityQueue<GroupTreeNode> children;
	private final int level;

	private GroupTreeNode(GroupTreeNode parent, Group group, int level)
	{
		this.parent = Optional.of(parent);
		this.group = group;
		this.level = level;
		this.children = new PriorityQueue<>(Comparator.comparing(GroupTreeNode::getDisplayedName));
	}

	public GroupTreeNode(Group group, int level)
	{
		this.parent = Optional.empty();
		this.group = group;
		this.level = level;
		this.children = new PriorityQueue<>(Comparator.comparing(GroupTreeNode::getDisplayedName));
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
		return group.displayedName;
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

	public boolean isDelegationEnableSubprojects()
	{
		return group.delegationEnableSubprojects;
	}

	public List<GroupTreeNode> getAllNodes()
	{
		List<GroupTreeNode> groups = new LinkedList<>();
		groups.add(this);
		children.forEach(groupTreeNode -> groups.addAll(groupTreeNode.getAllNodes()));
		return groups;
	}

	public List<GroupTreeNode> getAllChildrenElementsWithCutOff()
	{
		return cutOffParent().stream()
				.flatMap(groupTreeNode -> groupTreeNode.getAllNodes().stream())
				.collect(Collectors.toList());
	}

	public List<GroupTreeNode> getAllParentsElements()
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
