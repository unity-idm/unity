/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.endpoint.common.forms.groups;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.basic.Group;

import java.util.*;
import java.util.stream.Collectors;

public class GroupTreeNode
{
	private static Comparator<GroupTreeNode> groupComparator(MessageSource messageSource)
	{
		return (GroupTreeNode node1, GroupTreeNode node2) ->
		{
			int result = node1.getDisplayedName(messageSource).compareTo(node2.getDisplayedName(messageSource));
			if (areDisplayedNamesEquals(result))
				return node1.getPath().compareTo(node2.getPath());
			return result;
		};
	}

	private static boolean areDisplayedNamesEquals(int result)
	{
		return result == 0;
	}

	public final Group group;
	public final Optional<GroupTreeNode> parent;
	private final TreeSet<GroupTreeNode> children;
	private final int level;
	private final MessageSource messageSource;

	private GroupTreeNode(GroupTreeNode parent, Group group, int level, MessageSource messageSource)
	{
		this.parent = Optional.of(parent);
		this.group = group;
		this.level = level;
		this.messageSource = messageSource;
		this.children = new TreeSet<>(groupComparator(messageSource));
	}

	public GroupTreeNode(Group group, int level, MessageSource messageSource)
	{
		this.parent = Optional.empty();
		this.group = group;
		this.level = level;
		this.messageSource = messageSource;
		this.children = new TreeSet<>(groupComparator(messageSource));
	}

	public boolean addChild(Group group)
	{
		if(group.getPathEncoded().equals(this.group.getPathEncoded()))
			return false;
		if(!group.getPathEncoded().startsWith(this.group.getPathEncoded()))
			return false;
		for (GroupTreeNode child : children)
		{
			if(child.addChild(group))
				return true;
		}
		children.add(new GroupTreeNode(this, group, level + 1, messageSource));
		return true;
	}

	public String getPath()
	{
		return group.getPathEncoded();
	}

	public boolean isRoot()
	{
		return parent.isEmpty();
	}

	public int getLevel()
	{
		return level;
	}

	public String getDisplayedName(MessageSource msg)
	{
		return group.getDisplayedName().getValue(msg);
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
		GroupTreeNode node = new GroupTreeNode(group, 0, messageSource);
		children.forEach(child -> node.addChild(child.copyTree(node)));
		return node;
	}

	private GroupTreeNode copyTree(GroupTreeNode parent)
	{
		GroupTreeNode node = new GroupTreeNode(parent, group, parent.level + 1, messageSource);
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
