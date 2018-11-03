/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityViewBase;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Lists all routes
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class Routes extends UnityViewBase
{

	public static final String VIEW_NAME = "Routes";
	private UnityMessageSource msg;

	@Autowired
	public Routes(UnityMessageSource msg)
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
	public String getDisplayName()
	{

		return msg.getMessage("WebConsoleMenu.authentication.routes");
	}

	@Component
	public static class RoutesNavigationInfoProvider implements WebConsoleNavigationInfoProvider
	{
		private UnityMessageSource msg;
		private AuthenticationNavigationInfoProvider parent;
		private ObjectFactory<?> factory;

		@Autowired
		public RoutesNavigationInfoProvider(UnityMessageSource msg,
				AuthenticationNavigationInfoProvider parent,
				ObjectFactory<Routes> factory)
		{
			this.msg = msg;
			this.parent = parent;
			this.factory = factory;

		}

		@Override
		public NavigationInfo getNavigationInfo()
		{

			return new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo())
					.withObjectFactory(factory)
					.withCaption(msg.getMessage(
							"WebConsoleMenu.authentication.routes"))
					.build();
		}
	}
}
