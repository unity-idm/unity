/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.navigation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unity navigation structure manager. Contains map of
 * {@link NavigationInfoProvider}. Initializes the default view.
 * 
 * @author P.Piernik
 */
public class NavigationHierarchyManager
{
	private Map<String, NavigationInfo> navigationMap;
	private Map<String, List<NavigationInfo>> navigationChildren;

	public NavigationHierarchyManager(Collection<? extends NavigationInfoProvider> providers)
	{
		navigationMap = providers.stream().collect(Collectors
				.toMap(p -> p.getNavigationInfo().id, p -> p.getNavigationInfo()));
		initChildren();
		initDefaultView();
	}

	private void initDefaultView()
	{
		NavigationInfo defaultView = null;
		for (NavigationInfo info : navigationMap.values())
		{
			if (info.type == NavigationInfo.Type.DefaultView)
			{
				defaultView = info;
			}
		}
		if (defaultView != null)
			navigationMap.put("", defaultView);
	}

	private void initChildren()
	{
		navigationChildren = new HashMap<>();

		for (NavigationInfo view : navigationMap.values())
		{
			if (view.parent == null)
			{
				initChildren(view);
			}
		}
	}

	private void initChildren(NavigationInfo view)
	{
		List<NavigationInfo> children = navigationMap.values().stream()
				.filter(v -> v.parent != null && v.parent.id.equals(view.id))
				.collect(Collectors.toList());

		if (!navigationChildren.containsKey(view.id))
		{
			navigationChildren.put(view.id, children);
		}

		for (NavigationInfo child : children)
		{
			initChildren(child);
		}

	}

	public List<NavigationInfo> getParentPath(String viewName)
	{

		if (viewName != null && navigationMap.containsKey(viewName))
		{
			List<NavigationInfo> ret = getParentRecursive(navigationMap.get(viewName),
					new ArrayList<>());
			Collections.reverse(ret);
			return trimRootGroup(ret);
		} else
		{
			return Collections.emptyList();
		}

	}

	private List<NavigationInfo> trimRootGroup(List<NavigationInfo> ret)
	{
		return ret.size() > 1 ? ret.subList(1, ret.size()) : ret;
	}

	private List<NavigationInfo> getParentRecursive(NavigationInfo viewInfo,
			List<NavigationInfo> ret)
	{
		if (viewInfo == null)
			return ret;
		ret.add(viewInfo);
		if (viewInfo.parent == null)
			return ret;
		return getParentRecursive(viewInfo.parent, ret);
	}

	public List<NavigationInfo> getChildren(String viewName)
	{
		if (viewName != null && navigationChildren.containsKey(viewName))
		{

			List<NavigationInfo> ret = navigationChildren.get(viewName);
			ret.sort((c1, c2) -> {
				return c1.position - c2.position;
			});
			return ret;
		} else
		{
			return Collections.emptyList();
		}
	}

	public Map<String, NavigationInfo> getNavigationInfoMap()
	{
		return new HashMap<>(navigationMap);
	}
}
