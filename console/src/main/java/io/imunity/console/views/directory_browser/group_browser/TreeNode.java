/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_browser;

import com.vaadin.flow.data.provider.hierarchy.TreeData;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;

class TreeNode
{
	private final Group group;
	private final TreeNode parent;
	private final MessageSource msg;
	private String name;
	private boolean delegated;

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

	boolean anyFieldsOrChildContains(String searched, TreeData<TreeNode> treeData)
	{
		String textLower = searched.toLowerCase();

		if (toString().toLowerCase().contains(textLower))
			return true;

		if (group.getPathEncoded().toLowerCase().contains(textLower))
			return true;

		boolean anyChildContains = false;

		for (TreeNode child : treeData.getChildren(this))
		{
			anyChildContains |= child.anyFieldsOrChildContains(searched, treeData);
		}

		return anyChildContains;
	}

	boolean isChild(TreeNode parent)
	{
		return Group.isChild(group.getPathEncoded(), parent.group.getPathEncoded());
	}
}