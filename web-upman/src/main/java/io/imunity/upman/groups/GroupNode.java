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
	private UnityMessageSource msg;
	private String icon;
	private boolean publicGroup;

	public GroupNode(UnityMessageSource msg, Group group)
	{
		this(msg, group, null);
	}

	public GroupNode(UnityMessageSource msg, Group group, GroupNode parent)
	{
		this.msg = msg;
		this.path = group.toString();
		this.parent = parent;
		// TODO set icon based on group access mode
		this.publicGroup = true;
		this.icon = publicGroup ? Images.padlock_unlock.getHtml()
				: Images.padlock_lock.getHtml();
		setGroupMetadata(group);
	}

	public void setGroupMetadata(Group group)
	{

		this.name = group.getDisplayedName().getValue(msg);
		String realName = group.toString();
		if (realName.equals(name))
			this.name = group.getNameShort();

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

	public boolean isPublic()
	{
		return publicGroup;
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
