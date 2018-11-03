/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.userprofile;

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
 * User profile view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class UserProfile extends UnityViewBase
{
	public static String VIEW_NAME = "UserProfile";

	private UnityMessageSource msg;

	@Autowired
	public UserProfile(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		Label title = new Label();
		title.setValue("User profile");
		main.addComponent(title);
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayName()
	{
		return msg.getMessage("WebConsoleMenu.userProfile");
	}

	@Component
	public static class UserProfileNavigationInfoProvider
			implements WebConsoleNavigationInfoProvider
	{
		private UnityMessageSource msg;
		private RootNavigationInfoProvider parent;
		private ObjectFactory<?> factory;

		@Autowired
		public UserProfileNavigationInfoProvider(UnityMessageSource msg,
				RootNavigationInfoProvider parent, ObjectFactory<UserProfile> factory)
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
					.withCaption(msg.getMessage("WebConsoleMenu.userProfile"))
					.withIcon(Images.user.getResource())
					.withPosition(3).build();
		}
	}
}
