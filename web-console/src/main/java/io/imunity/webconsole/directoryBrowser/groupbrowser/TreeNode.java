/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.groupbrowser;

import com.vaadin.data.TreeData;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.basic.Group;

class TreeNode
{
	private String name;
	private Group group;
	private TreeNode parent;
	private MessageSource msg;
	private boolean delegated;

	TreeNode(MessageSource msg, Group group)
	{
		this(msg, group, null);
	}

	TreeNode(MessageSource msg, Group group, TreeNode parent)
	{
		this.msg = msg;
		this.group = group;
		this.parent = parent;
		setGroupMetadata(group);
	}

	void setGroupMetadata(Group group)
	{
		this.setDelegated(group.getDelegationConfiguration().enabled);
		if (group.isTopLevel())
		{
			this.name = group.getDisplayedName().getValue(msg);
			if (this.name.equals("/"))
				this.name = msg.getMessage("GroupBrowser.root");
			else
				this.name = name + " (/)";
		} else
		{
			this.name = group.getDisplayedNameShort(msg).getValue(msg);
		}
	}
	
	public boolean isDelegated()
	{
		return delegated;
	}

	public void setDelegated(boolean delegated)
	{
		this.delegated = delegated;
	}
	
	TreeNode getParentNode()
	{
		return parent;
	}

	Group getGroup()
	{
		return group;
	}

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public int hashCode()
	{
		return group.getPathEncoded().hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		String path = group.getPathEncoded();
		if (obj instanceof String)
			return path.equals(obj);
		if (obj instanceof TreeNode)
			return path.equals(((TreeNode) obj).group.getPathEncoded());
		return false;
	}

	boolean anyFieldsOrChildContains(String searched, MessageSource msg, TreeData<TreeNode> treeData)
	{
		String textLower = searched.toLowerCase();

		if (toString().toLowerCase().contains(textLower))
			return true;

		if (group.getPathEncoded().toLowerCase().contains(textLower))
			return true;

		boolean anyChildContains = false;

		for (TreeNode child : treeData.getChildren(this))
		{
			anyChildContains |= child.anyFieldsOrChildContains(searched, msg, treeData);
		}

		return anyChildContains;
	}

	boolean isChild(TreeNode parent)
	{
		return Group.isChild(group.getPathEncoded(), parent.group.getPathEncoded());
	}
}