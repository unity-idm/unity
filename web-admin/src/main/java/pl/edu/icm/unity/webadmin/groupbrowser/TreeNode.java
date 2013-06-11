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
	private UnityMessageSource msg;
	private String name;
	private String path;
	private String parentPath;
	private boolean contentsFetched = false;
	
	public TreeNode(UnityMessageSource msg, String path)
	{
		this(msg, path, false);
	}
	
	public TreeNode(UnityMessageSource msg, String path, boolean forceRoot)
	{
		this.msg = msg;
		this.path = path;
		if (!forceRoot)
		{
			Group tmp = new Group(path);
			this.name = tmp.isTopLevel() ? msg.getMessage("GroupBrowser.root") : tmp.getName();
			this.parentPath = tmp.getParentPath();
		} else
		{
			this.name = path;
			this.parentPath = null;
		}
	}
	
	public TreeNode getParentNode()
	{
		return parentPath == null ? null : new TreeNode(msg, parentPath);
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