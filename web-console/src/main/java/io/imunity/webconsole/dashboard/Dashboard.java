/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.dashboard;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.RootNavigationInfoProvider;
import io.imunity.webconsole.WebConsoleNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityViewBase;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Default dashbord view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class Dashboard extends UnityViewBase
{

	public static final String VIEW_NAME = "Dashboard";

	private UnityMessageSource msg;

	@Autowired
	public Dashboard(UnityMessageSource msg)
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
	public String getDisplayName()
	{
		return msg.getMessage("WebConsoleMenu.dashboard");
	}

	@Component
	public class DashboardGroupInfoProvider implements WebConsoleNavigationInfoProvider
	{
		private UnityMessageSource msg;
		private RootNavigationInfoProvider parent;
		private ObjectFactory<?> factory;

		@Autowired
		public DashboardGroupInfoProvider(UnityMessageSource msg,
				RootNavigationInfoProvider parent, ObjectFactory<Dashboard> factory)
		{
			this.msg = msg;
			this.parent = parent;
			this.factory = factory;

		}

		@Override
		public NavigationInfo getNavigationInfo()
		{

			return new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.DefaultView)
					.withParent(parent.getNavigationInfo())
					.withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.dashboard"))
					.withIcon(Images.dashboard.getResource())
					.withPosition(0).build();
		}

	}

}
