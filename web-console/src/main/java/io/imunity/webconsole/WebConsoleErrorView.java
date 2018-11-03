/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityViewBase;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Default error view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class WebConsoleErrorView extends UnityViewBase
{

	public static final String VIEW_NAME = "Error";

	private UnityMessageSource msg;

	@Autowired
	public WebConsoleErrorView(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		Label title = new Label();
		title.setValue(msg.getMessage("error"));
		main.addComponent(title);
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayName()
	{
		return msg.getMessage("error");
	}

	@Component
	public class WebConsoleErrorViewInfoProvider implements WebConsoleNavigationInfoProvider
	{
		private ObjectFactory<?> factory;

		@Autowired
		public WebConsoleErrorViewInfoProvider(ObjectFactory<WebConsoleErrorView> factory)
		{
			this.factory = factory;

		}

		@Override
		public NavigationInfo getNavigationInfo()
		{

			return new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(null).withObjectFactory(factory).build();
		}

	}
}
