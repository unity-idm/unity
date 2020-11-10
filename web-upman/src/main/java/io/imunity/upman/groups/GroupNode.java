/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.groups;

import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Tree node used in {@link GroupsTree}
 * 
 * @author P.Piernik
 *
 */
class GroupNode
{
	public final DelegatedGroup group;
	public final GroupNode parent;
	public final String htmlPrivacyIcon;
	public final String htmlIcon;

	public GroupNode(DelegatedGroup group)
	{
		this(group, null);
	}

	public GroupNode(DelegatedGroup group, GroupNode parent)
	{
		this.group = group;
		this.parent = parent;
		this.htmlPrivacyIcon = group.open ? Images.padlock_unlock.getHtml() : "";
		this.htmlIcon = group.delegationConfiguration.enabled && group.delegationConfiguration.logoUrl != null
				&& !group.delegationConfiguration.logoUrl.isEmpty()
						? "<img class=\"logo\" src=\"" + group.delegationConfiguration.logoUrl + "\">"
							
						: "";
	}

	public String getPath()
	{
		return group.path;
	}

	@Override
	public String toString()
	{
		return group.displayedName;
	}

	public boolean isOpen()
	{
		return group.open;
	}

	@Override
	public int hashCode()
	{
		return group.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof String)
			return group.path.equals(obj);
		if (obj instanceof GroupNode)
			return group.path.equals(((GroupNode) obj).group.path);
		return false;
	}
}
