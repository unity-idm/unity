/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directoryBrowser;

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
 * Default dashbord view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class DirectoryBrowser extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "DirectoryBrowser";

	private UnityMessageSource msg;

	@Autowired
	public DirectoryBrowser(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		Label title = new Label();
		title.setValue("Directory browser");
		main.addComponent(title);
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.directoryBrowser");
	}
	
	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public class DirectoryBrowserNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public DirectoryBrowserNavigationInfoProvider(UnityMessageSource msg,
				WebConsoleRootNavigationInfoProvider parent,
				ObjectFactory<DirectoryBrowser> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.DefaultView)
					.withParent(parent.getNavigationInfo())
					.withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.directoryBrowser"))
					.withIcon(Images.folder.getResource()).withPosition(10)
					.build());

		}
	}

}
