/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.I18nString;

public class MVELGroup
{
	private final Group group;
	private final MVELGroup parent;

	public MVELGroup(Group group, Function<String, Group> groupProvider)
	{
		this.group = group;
		if (group.isTopLevel())
		{
			this.parent = null;
		} else
		{
			this.parent = new MVELGroup(groupProvider.apply(group.getParentPath()), groupProvider);
		}
	}

	public boolean isChild(MVELGroup test)
	{
		return group.isChild(test.group);
	}

	public boolean isChildNotSame(MVELGroup test)
	{
		return Group.isChild(toString(), test.group.toString(), false);
	}

	public boolean isTopLevel()
	{
		return group.isTopLevel();
	}

	public String getPathEncoded()
	{
		return group.getPathEncoded();
	}

	public String getName()
	{
		return group.getName();
	}

	public I18nString getDisplayedNameShort(MessageSource msg)
	{
		return group.getDisplayedNameShort(msg);
	}

	public String getRelativeName()
	{
		return group.getRelativeName();
	}

	public String getParentPath()
	{
		return group.getParentPath();
	}

	public AttributeStatement[] getAttributeStatements()
	{
		return group.getAttributeStatements();
	}

	public Set<String> getAttributesClasses()
	{
		return group.getAttributesClasses();
	}

	public GroupDelegationConfiguration getDelegationConfiguration()
	{
		return group.getDelegationConfiguration();
	}

	public boolean isPublic()
	{
		return group.isPublic();
	}

	public String getNameShort()
	{
		return group.getNameShort();
	}

	public String toString()
	{
		return group.toString();
	}

	public I18nString getDescription()
	{
		return group.getDescription();
	}

	public I18nString getDisplayedName()
	{
		return group.getDisplayedName();
	}

	public Map<String, String> getProperties()
	{
		return group.getProperties().entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().value));
	}

	public String getEncodedGroupPath(String delimiter, Function<MVELGroup, String> groupProvider)
	{
		if (parent == null)
		{
			return groupProvider.apply(this);
		} else
		{
			return parent.getEncodedGroupPath(delimiter, groupProvider) + delimiter + groupProvider.apply(this);
		}
	}
}
