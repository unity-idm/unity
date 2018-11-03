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
 * {@link NavigationInfoProvider}. Initializes the default view
 * 
 * @author P.Piernik
 *
 */

public class NavigationManager
{

	private Map<String, NavigationInfo> navigationMap;
	private Map<String, List<NavigationInfo>> navigationChildren;

	public NavigationManager(Collection<? extends NavigationInfoProvider> providers)
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
				initChilden(view);
			}
		}
	}

	private void initChilden(NavigationInfo view)
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
			initChilden(child);
		}

	}

	public List<NavigationInfo> getParentPath(String viewName)
	{
		List<NavigationInfo> ret = new ArrayList<>();

		if (viewName != null && navigationMap.containsKey(viewName))
		{
			getParent(navigationMap.get(viewName), ret);
		}

		Collections.reverse(ret);
		return trimRootGroup(ret);

	}

	private List<NavigationInfo> trimRootGroup(List<NavigationInfo> ret)
	{
		return ret.size() > 1 ? ret.subList(1, ret.size()) : ret;
	}

	private List<NavigationInfo> getParent(NavigationInfo viewInfo, List<NavigationInfo> ret)
	{
		if (viewInfo == null)
			return ret;
		ret.add(viewInfo);
		if (viewInfo.parent == null)
			return ret;
		return getParent(viewInfo.parent, ret);
	}

	public List<NavigationInfo> getChildren(String viewName)
	{

		List<NavigationInfo> ret = navigationChildren.get(viewName);
		ret.sort((c1, c2) -> {
			return c1.position - c2.position;
		});
		return ret;
	}

	public Map<String, NavigationInfo> getNavigationInfoMap()
	{
		return navigationMap;
	}
}
