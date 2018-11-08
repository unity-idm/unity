/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.navigation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;

import pl.edu.icm.unity.base.utils.Log;

/**
 * Provides unity view based on {@link NavigationHierarchyManager}
 * 
 * @author P.Piernik
 *
 */
public class AppContextViewProvider implements ViewProvider
{

	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB,
			AppContextViewProvider.class);

	private Map<String, ObjectFactory<?>> viewFactories;

	@Autowired
	public AppContextViewProvider(NavigationHierarchyManager navMan)
	{
		viewFactories = new HashMap<>();

		for (Entry<String, NavigationInfo> navEntry : navMan.getNavigationInfoMap()
				.entrySet())
		{
			viewFactories.put(navEntry.getKey(), navEntry.getValue().objectFactory);
		}
	}

	@Override
	public String getViewName(String viewAndParameters)
	{

		if (!viewAndParameters.isEmpty())
		{
			for (String view : viewFactories.keySet().stream().filter(v -> !v.isEmpty())
					.collect(Collectors.toSet()))
			{
				if (getViewNameInternal(viewAndParameters).equals(view))
					return view;
			}
		}
		return viewAndParameters;
	}
	
	private String getViewNameInternal(String viewAndParameters)
	{
		if (viewAndParameters.contains("/"))
		{
			return viewAndParameters.split("/")[0];
		}
		
		return viewAndParameters;	
	}

	@Override
	public View getView(String viewName)
	{
		if (viewName != null && viewFactories.containsKey(viewName))
		{
			try
			{
				return (View) viewFactories.get(viewName).getObject();
			} catch (Exception e)

			{
				LOG.debug("Cannot load view " + viewName, e);
			}
		} else
		{
			LOG.debug("View " + viewName + " not exits");
		}

		return null;
	}

}
