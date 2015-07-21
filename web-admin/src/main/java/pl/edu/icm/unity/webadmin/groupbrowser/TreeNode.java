/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Group;

/**
 * 
 * @author K. Benedyczak
 */
public class TreeNode
{
	private String name;
	private String path;
	private TreeNode parent;
	private boolean contentsFetched = false;
	private UnityMessageSource msg;
	
	public TreeNode(UnityMessageSource msg, Group group)
	{
		this(msg, group, null);
	}
	
	public TreeNode(UnityMessageSource msg, Group group, TreeNode parent)
	{
		this.msg = msg;
		this.path = group.toString();
		this.parent = parent;
		setGroupMetadata(group);
	}
	
	public void setGroupMetadata(Group group)
	{
		if (group.isTopLevel())
		{
			this.name = group.getDisplayedNameShort().getValue(msg);
			if (this.name.equals("/"))
				this.name = msg.getMessage("GroupBrowser.root");
			else
				this.name = name + " (/)";
		} else
		{
			this.name = group.getDisplayedNameShort().getValue(msg);
			String realName = group.getName();
			if (!realName.equals(name))
				this.name = name + " (" + realName + ")";
		}
	}
	
	public TreeNode getParentNode()
	{
		return parent;
	}
	
	public boolean isContentsFetched()
	{
		return contentsFetched;
	}

	public void setContentsFetched(boolean contentsFetched)
	{
		this.contentsFetched = contentsFetched;
	}

	public String getPath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
	}

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public int hashCode()
	{
		return path.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof String)
			return path.equals(obj);
		if (obj instanceof TreeNode)
			return path.equals(((TreeNode)obj).path);
		return false;
	}
}