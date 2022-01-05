/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mvel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableMap;

import pl.edu.icm.unity.types.basic.Group;

public class CachingMVELGroupProvider
{
	private final Map<String, Group> groups;
	private final Map<String, MVELGroup> cache;
	
	public CachingMVELGroupProvider(Map<String, Group> groups)
	{
		this.groups = ImmutableMap.copyOf(groups);
		cache = new ConcurrentHashMap<>(groups.size());
	}

	public MVELGroup get(String groupPath)
	{
		return cache.computeIfAbsent(groupPath, path -> new MVELGroup(groups.get(path), this::get));
	}
}
