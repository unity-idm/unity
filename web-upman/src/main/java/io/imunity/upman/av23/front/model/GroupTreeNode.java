/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.model;

import java.util.*;

public class GroupTreeNode
{
	private final Group group;
	private final TreeSet<GroupTreeNode> children;
	private final int level;

	public GroupTreeNode(Group group, int level)
	{
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
		children.add(new GroupTreeNode(group, level + 1));
		return true;
	}

	public String getPath()
	{
		return group.path;
	}

	public boolean isBaseLevel()
	{
		return level == 0;
	}

	public String getDisplayedName()
	{
		return group.displayedName;
	}

	public List<GroupTreeNode> getChildren()
	{
		return List.copyOf(children);
	}

	public List<Group> getAllChildren()
	{
		List<Group> groups = new LinkedList<>();
		groups.add(new Group(group.path, group.displayedName, group.delegationEnabled, level));
		children.forEach(groupTreeNode -> groups.addAll(groupTreeNode.getAllChildren()));
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
