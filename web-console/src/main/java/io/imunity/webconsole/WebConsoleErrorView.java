/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Default web console error view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class WebConsoleErrorView extends CustomComponent implements UnityView
{

	public static final String VIEW_NAME = "Error";

	private MessageSource msg;

	@Autowired
	public WebConsoleErrorView(MessageSource msg)
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
	public String getDisplayedName()
	{
		return msg.getMessage("error");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}
	
	@Component
	public class WebConsoleErrorViewInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public WebConsoleErrorViewInfoProvider(ObjectFactory<WebConsoleErrorView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(null).withObjectFactory(factory).build());

		}
	}
}
