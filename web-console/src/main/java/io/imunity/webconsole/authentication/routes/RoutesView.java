/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.routes;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.AuthenticationNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Lists all routes
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class RoutesView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "Routes";
	
	private UnityMessageSource msg;

	@Autowired
	public RoutesView(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{

		VerticalLayout main = new VerticalLayout();
		Label title = new Label();
		title.setValue("Routes main");
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{

		return msg.getMessage("WebConsoleMenu.authentication.routes");
	}
	
	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class RoutesNavigationInfoProvider
			extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public RoutesNavigationInfoProvider(UnityMessageSource msg,
				AuthenticationNavigationInfoProvider parent,
				ObjectFactory<RoutesView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo())
					.withObjectFactory(factory)
					.withCaption(msg.getMessage(
							"WebConsoleMenu.authentication.routes"))
					.build());

		}
	}
}
