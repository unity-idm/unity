/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.groups;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Tree node used in {@link GroupsTree}
 * 
 * @author P.Piernik
 *
 */
public class GroupNode
{
	private String name;
	private String path;
	private GroupNode parent;
	private String icon;
	private boolean open;

	public GroupNode(UnityMessageSource msg, Group group)
	{
		this(msg, group, null);
	}

	public GroupNode(UnityMessageSource msg, Group group, GroupNode parent)
	{
		this.path = group.toString();
		this.parent = parent;
		this.open = group.isOpen();
		this.icon = open ? Images.padlock_unlock.getHtml()
				: Images.padlock_lock.getHtml();
		this.name = group.getDisplayedName().getValue(msg);;
	}

	public String getIcon()
	{
		return icon;
	}

	public void setIcon(String icon)
	{
		this.icon = icon;
	}

	public GroupNode getParentNode()
	{
		return parent;
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

	public boolean isOpen()
	{
		return open;
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
		if (obj instanceof GroupNode)
			return path.equals(((GroupNode) obj).path);
		return false;
	}
}
