/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.model;

import java.util.*;

public class GroupTreeNode
{
	public final Group group;
	public final Optional<GroupTreeNode> parent;
	private final TreeSet<GroupTreeNode> children;
	private final int level;

	private GroupTreeNode(GroupTreeNode parent, Group group, int level)
	{
		this.parent = Optional.of(parent);
		this.group = group;
		this.level = level;
		this.children = new TreeSet<>(Comparator.comparing(GroupTreeNode::getDisplayedName));
	}

	public GroupTreeNode(Group group, int level)
	{
		this.parent = Optional.empty();
		this.group = group;
		this.level = level;
		this.children = new TreeSet<>(Comparator.comparing(GroupTreeNode::getDisplayedName));
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

	public List<Group> getAllElements()
	{
		List<Group> groups = new LinkedList<>();
		groups.add(new Group(group));
		children.forEach(groupTreeNode -> groups.addAll(groupTreeNode.getAllElements()));
		return groups;
	}

	public List<GroupTreeNode> getAllNodes()
	{
		List<GroupTreeNode> groups = new LinkedList<>();
		groups.add(this);
		children.forEach(groupTreeNode -> groups.addAll(groupTreeNode.getAllNodes()));
		return groups;
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
