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

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.WebConsoleRootNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Other service view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class OtherServicesView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "OtherServices";

	private UnityMessageSource msg;

	@Autowired
	public OtherServicesView(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		Label title = new Label();
		title.setValue("Other services");

		main.addComponent(title);
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{

		return msg.getMessage("WebConsoleMenu.otherServices");
	}
	
	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class OtherServicesNavigationInfoProvider
			extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public OtherServicesNavigationInfoProvider(UnityMessageSource msg,
				WebConsoleRootNavigationInfoProvider parent,
				ObjectFactory<OtherServicesView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo())
					.withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.otherServices"))
					.withIcon(Images.question.getResource()).withPosition(4)
					.build());

		}
	}
}
