/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.other;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.RootNavigationInfoProvider;
import io.imunity.webconsole.WebConsoleNavigationInfoProvider;
import io.imunity.webconsole.dashboard.Dashboard;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Other service view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class OtherServices extends CustomComponent implements UnityView
{

	public static String VIEW_NAME = "OtherServices";

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		Label title = new Label();
		title.setValue("Other services");

		main.addComponent(title);
		setCompositionRoot(main);
	}

	@Component
	public static class OtherServicesNavigationInfoProvider
			implements WebConsoleNavigationInfoProvider
	{
		private UnityMessageSource msg;
		private RootNavigationInfoProvider parent;
		private ObjectFactory<?> factory;

		@Autowired
		public OtherServicesNavigationInfoProvider(UnityMessageSource msg,
				RootNavigationInfoProvider parent, ObjectFactory<Dashboard> factory)
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
					.withDisplayNameProvider(e -> msg
							.getMessage("WebConsoleMenu.otherServices"))
					.withPosition(4).build();
		}
	}
}
