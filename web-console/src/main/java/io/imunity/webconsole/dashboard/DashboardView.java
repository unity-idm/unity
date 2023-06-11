/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.dashboard;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Default dashbord view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class DashboardView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "Dashboard";

	private MessageSource msg;

	@Autowired
	public DashboardView(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		Label title = new Label();
		title.setValue("Welcome in Unity Web Console");
		main.addComponent(title);
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.dashboard");
	}
	
	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

//	@Component
//	public class DashboardNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
//	{
//		@Autowired
//		public DashboardNavigationInfoProvider(UnityMessageSource msg,
//				WebConsoleRootNavigationInfoProvider parent,
//				ObjectFactory<DashboardView> factory)
//		{
//			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.DefaultView)
//					.withParent(parent.getNavigationInfo())
//					.withObjectFactory(factory)
//					.withCaption(msg.getMessage("WebConsoleMenu.dashboard"))
//					.withIcon(Images.dashboard.getResource()).withPosition(0)
//					.build());
//
//		}
//	}

}
