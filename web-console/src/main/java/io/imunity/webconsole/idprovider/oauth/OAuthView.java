/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.idprovider.oauth;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.idprovider.IdpNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * OAuth view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class OAuthView extends CustomComponent implements UnityView
{

	public static final String VIEW_NAME = "OAuth";

	private UnityMessageSource msg;

	@Autowired
	public OAuthView(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		Label title = new Label();
		title.setValue("OAuth");
		main.addComponent(title);
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{

		return msg.getMessage("WebConsoleMenu.idpProvider.oauth");
	}
	
	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class OAuthNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public OAuthNavigationInfoProvider(UnityMessageSource msg,
				IdpNavigationInfoProvider parent, ObjectFactory<OAuthView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo())
					.withObjectFactory(factory)
					.withCaption(msg.getMessage(
							"WebConsoleMenu.idpProvider.oauth"))
					.build());

		}
	}

}
