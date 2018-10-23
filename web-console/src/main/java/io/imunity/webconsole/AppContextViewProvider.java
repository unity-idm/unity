/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;

import pl.edu.icm.unity.base.utils.Log;

@Component
public class AppContextViewProvider implements ViewProvider
{

	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB,
			AppContextViewProvider.class);

	private ApplicationContext context;
	private Map<String, Class<?>> viewList;

	@PostConstruct
	void init()
	{

		viewList = new HashMap<String, Class<?>>();
		for (String view : context.getBeanNamesForType(View.class))
		{
			viewList.put(context.getType(view).getSimpleName(), context.getType(view));
		}
	}

	@Autowired
	public AppContextViewProvider(ApplicationContext context)
	{
		this.context = context;
	}

	@Override
	public String getViewName(String viewAndParameters)
	{
		return viewAndParameters;
	}

	@Override
	public View getView(String viewName)
	{
		if (viewName != null && !viewName.isEmpty())
		{
			try
			{
				return (View) context.getBean(viewList.get(viewName));
			} catch (Exception e)

			{
				LOG.debug("Cannot load view " + viewName, e);
			}
		}

		return null;
	}

}
