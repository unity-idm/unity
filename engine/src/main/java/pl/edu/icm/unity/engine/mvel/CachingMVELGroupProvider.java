/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mvel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.types.basic.Group;

public class CachingMVELGroupProvider
{
	private final Map<String, MVELGroup> cache;
	
	public CachingMVELGroupProvider(Map<String, Group> groups)
	{
		cache = new HashMap<>(groups.size());

		List<String> groupPaths = new ArrayList<>(groups.keySet());
		Collections.sort(groupPaths, (a, b) -> Integer.compare(a.length(), b.length()));
		for (String path: groupPaths)
		{
			Group group = groups.get(path);
			String parentPath = group.getParentPath();
			cache.put(path, new MVELGroup(group, parentPath == null ? null : cache.get(parentPath)));
		}
	}

	public MVELGroup get(String groupPath)
	{
		MVELGroup ret = cache.get(groupPath);
		if (ret == null)
			throw new IllegalArgumentException("No cached MVEL group for path " + groupPath);
		return ret;
	}
}
