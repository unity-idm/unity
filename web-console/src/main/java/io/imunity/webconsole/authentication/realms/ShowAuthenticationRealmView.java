/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import io.imunity.webconsole.WebConsoleNavigationInfoProvider;
import io.imunity.webconsole.authentication.realms.AuthenticationRealmsView.RealmsNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityViewBase;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.ControllerException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Show realm view
 * 
 * @author P.Piernik
 *
 */
@Component
public class ShowAuthenticationRealmView extends UnityViewBase
{

	public static String VIEW_NAME = "ViewAuthenticationRealm";

	private AuthenticationRealmController controller;
	private UnityMessageSource msg;
	private String realmName;
	
	@Autowired
	public ShowAuthenticationRealmView(UnityMessageSource msg, AuthenticationRealmController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		FormLayout main = new FormLayout();

		realmName = getParam(event, "name");

		AuthenticationRealm realm;
		try
		{
			realm = controller.getRealm(realmName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			UI.getCurrent().getNavigator().navigateTo(AuthenticationRealmsView.VIEW_NAME);
			return;
		}

		Label name = new Label(realm.getName());
		name.setCaption(msg.getMessage("AuthenticationRealm.name"));
		main.addComponent(name);

		Label desc = new Label(realm.getDescription());
		desc.setCaption(msg.getMessage("AuthenticationRealm.description"));
		main.addComponent(desc);

		Label blockFor = new Label(String.valueOf(realm.getBlockFor()));
		blockFor.setCaption(msg.getMessage("AuthenticationRealm.blockFor"));
		main.addComponent(blockFor);

		Label blockAfterUnsuccessfulLogins = new Label(
				String.valueOf(realm.getBlockAfterUnsuccessfulLogins()));
		blockAfterUnsuccessfulLogins.setCaption(
				msg.getMessage("AuthenticationRealm.blockAfterUnsuccessfulLogins"));
		main.addComponent(blockAfterUnsuccessfulLogins);

		Label maxInactivity = new Label(String.valueOf(realm.getMaxInactivity()));
		maxInactivity.setCaption(msg.getMessage("AuthenticationRealm.maxInactivity"));
		main.addComponent(maxInactivity);

		Label allowForRememberMeDays = new Label(
				String.valueOf(realm.getAllowForRememberMeDays()));
		allowForRememberMeDays.setCaption(
				msg.getMessage("AuthenticationRealm.allowForRememberMeDays"));
		main.addComponent(allowForRememberMeDays);

		Label rememberMePolicy = new Label(realm.getRememberMePolicy().toString());
		rememberMePolicy.setCaption(msg.getMessage("AuthenticationRealm.rememberMePolicy"));
		main.addComponent(rememberMePolicy);

		setCompositionRoot(main);
	}

	@Override
	public String getDisplayName()
	{
		return realmName;
	}
	
	@org.springframework.stereotype.Component
	public static class ViewRealmNavigationInfoProvider
			implements WebConsoleNavigationInfoProvider
	{
		private RealmsNavigationInfoProvider parent;
		private ObjectFactory<?> factory;

		@Autowired
		public ViewRealmNavigationInfoProvider(RealmsNavigationInfoProvider parent,
				ObjectFactory<ShowAuthenticationRealmView> factory)
		{
			this.parent = parent;
			this.factory = factory;

		}

		@Override
		public NavigationInfo getNavigationInfo()
		{

			return new NavigationInfo.NavigationInfoBuilder(VIEW_NAME,
					Type.ParameterizedView)
							.withParent(parent.getNavigationInfo())
							.withObjectFactory(factory)
							.build();
		}
	}
}
