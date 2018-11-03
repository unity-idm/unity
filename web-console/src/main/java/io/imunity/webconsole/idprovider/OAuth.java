/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.idprovider;

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
 * OAuth view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class OAuth extends UnityViewBase
{

	public static String VIEW_NAME = "OAuth";

	private UnityMessageSource msg;

	@Autowired
	public OAuth(UnityMessageSource msg)
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

	@Component
	public static class OAuthViewInfoProvider implements WebConsoleNavigationInfoProvider
	{
		private UnityMessageSource msg;
		private IdpNavigationInfoProvider parent;
		private ObjectFactory<?> factory;

		@Autowired
		public OAuthViewInfoProvider(UnityMessageSource msg,
				IdpNavigationInfoProvider parent, ObjectFactory<OAuth> factory)
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
							"WebConsoleMenu.idpProvider.oauth"))
					.build();
		}
	}

	@Override
	public String getDisplayName()
	{

		return msg.getMessage("WebConsoleMenu.idpProvider.oauth");
	}
}
