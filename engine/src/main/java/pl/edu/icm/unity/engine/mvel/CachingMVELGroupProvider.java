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
	private final Map<String, MVELGroup> mvelGroupsByPathCache;
	
	public CachingMVELGroupProvider(Map<String, Group> groupsByPath)
	{
		mvelGroupsByPathCache = new HashMap<>(groupsByPath.size());

		List<String> groupPaths = new ArrayList<>(groupsByPath.keySet());
		Collections.sort(groupPaths, (a, b) -> Integer.compare(a.length(), b.length()));
		for (String path: groupPaths)
		{
			Group group = groupsByPath.get(path);
			String parentPath = group.getParentPath();
			mvelGroupsByPathCache.put(path, new MVELGroup(group, parentPath == null ? 
					null : mvelGroupsByPathCache.get(parentPath)));
		}
	}

	public MVELGroup get(String groupPath)
	{
		MVELGroup ret = mvelGroupsByPathCache.get(groupPath);
		if (ret == null)
			throw new IllegalArgumentException("No cached MVEL group for path " + groupPath);
		return ret;
	}
}
