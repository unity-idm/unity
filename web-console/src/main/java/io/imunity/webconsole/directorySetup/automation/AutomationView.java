/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.automation;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.directorySetup.DirectorySetupNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * 
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class AutomationView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "Automation";

	private UnityMessageSource msg;
	

	@Autowired
	AutomationView(UnityMessageSource msg)
	{
		this.msg = msg;
		
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.directorySetup.automation");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class AutomationNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public AutomationNavigationInfoProvider(UnityMessageSource msg,
				DirectorySetupNavigationInfoProvider parent,
				ObjectFactory<AutomationView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.directorySetup.automation"))
					.withPosition(40).build());

		}
	}
}
