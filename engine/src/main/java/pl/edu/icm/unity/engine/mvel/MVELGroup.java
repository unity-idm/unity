/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.mvel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mvel2.ast.PrototypalFunctionInstance;
import org.mvel2.integration.impl.MapVariableResolverFactory;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.GroupsChain;

public class MVELGroup
{

	private final Group group;
	private final MVELGroup parent;

	public MVELGroup(GroupsChain groupChain)
	{
		this.group = groupChain.getLast();
		GroupsChain parentChain = groupChain.getParentChain();
		this.parent = parentChain == null ? null : new MVELGroup(parentChain);
	}

	public boolean isChild(MVELGroup test)
	{
		return group.isChild(test.group);
	}

	public boolean isChildNotSame(MVELGroup test)
	{
		return group.isChildNotSame(test.group);
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
	
	public String getEncodedGroupPath(String delimiter, PrototypalFunctionInstance lambda)
	{
		Map<String, Object> var = new HashMap<>();
		var.put("group", this);
		MVELGroup[] group = new MVELGroup[1];
		group[0] = this;

		if (parent == null)
		{

			return (String) lambda.call(null, null, new MapVariableResolverFactory(), group);
		} else
		{
			return parent.getEncodedGroupPath(delimiter, lambda) + delimiter
					+ (String) lambda.call(null, null, new MapVariableResolverFactory(), group);
		}
	}
}
