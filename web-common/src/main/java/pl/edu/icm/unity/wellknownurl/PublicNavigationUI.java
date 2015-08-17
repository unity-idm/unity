/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.wellknownurl;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.common.ErrorComponent;

import com.vaadin.annotations.Theme;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.VaadinRequest;

/**
 * The Vaadin UI providing a concrete view depending on URL fragment. Actual views are configured via DI.
 * This variant is for use with unprotected resources, i.e. those not requiring prior authentication.
 * 
 * @author K. Benedyczak
 */
@Component("PublicNavigationUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class PublicNavigationUI extends UnityUIBase implements UnityWebUI
{
	private Navigator navigator;
	private Collection<PublicViewProvider> viewProviders;
	
	@Autowired
	public PublicNavigationUI(UnityMessageSource msg, Collection<PublicViewProvider> viewProviders)
	{
		super(msg);
		this.viewProviders = viewProviders;
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		navigator = new Navigator(this, this);
		for (PublicViewProvider viewProvider: viewProviders)
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


