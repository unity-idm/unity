/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupbrowser;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
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
	private String icon;
	
	public TreeNode(UnityMessageSource msg, Group group, String icon)
	{
		this(msg, group, icon, null);
	}
	
	public TreeNode(UnityMessageSource msg, Group group, String icon, TreeNode parent)
	{
		this.msg = msg;
		this.path = group.toString();
		this.parent = parent;
		this.icon = icon;
		setGroupMetadata(group);
	}
	
	public void setGroupMetadata(Group group)
	{
		if (group.isTopLevel())
		{
			this.name = group.getDisplayedName().getValue(msg);
			if (this.name.equals("/"))
				this.name = msg.getMessage("GroupBrowser.root");
			else
				this.name = name + " (/)";
		} else
		{
			this.name = group.getDisplayedName().getValue(msg);
			String realName = group.toString();
			if (!realName.equals(name))
				this.name = name + " (" + realName + ")";
		}
	}
	
	public String getIcon()
	{
		return icon;
	}

	public void setIcon(String icon)
	{
		this.icon = icon;
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