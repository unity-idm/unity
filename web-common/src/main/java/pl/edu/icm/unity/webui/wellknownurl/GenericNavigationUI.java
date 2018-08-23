/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.wellknownurl;

import java.util.Collection;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.VaadinRequest;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.common.ErrorComponent;

/**
 * The Vaadin UI providing a concrete view depending on URL fragment. Actual views are configured via DI.
 * Extensions should declare what type of URL providers they are interested in.
 * 
 * @author K. Benedyczak
 */
abstract class GenericNavigationUI<T extends ViewProvider> extends UnityUIBase implements UnityWebUI
{
	protected Navigator navigator;
	protected Collection<T> viewProviders;
	
	public GenericNavigationUI(UnityMessageSource msg, Collection<T> viewProviders)
	{
		super(msg);
		this.viewProviders = viewProviders;
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		navigator = new Navigator(this, this);
		for (T viewProvider : viewProviders)
			navigator.addProvider(viewProvider);
		navigator.setErrorView(new ErrorView());
	}
	
	public static class ErrorView extends ErrorComponent implements View 
	{
		public ErrorView()
		{
			setError("Nothing here...");
		}
		
		@Override
		public void enter(ViewChangeEvent event)
		{
		}
	}
}


