/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProvider;
import io.imunity.webconsole.authentication.realms.AuthenticationRealmsView.RealmsNavigationInfoProvider;
import io.imunity.webelements.helpers.ConfirmViewHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.ControllerException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Add realm view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class NewAuthenticationRealmView extends CustomComponent implements UnityView
{

	public static String VIEW_NAME = "NewAuthenticationRealm";

	private AuthenticationRealmController controller;
	private AuthenticationRealmEditor editor;
	private UnityMessageSource msg;

	@Autowired
	public NewAuthenticationRealmView(UnityMessageSource msg, AuthenticationRealmController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	private void onConfirm()
	{
		if (editor.hasErrors())
		{
			return;
		}

		try
		{
			if (!controller.addRealm(editor.getAuthenticationRealm()))
				return;
		} catch (ControllerException e)
		{

			NotificationPopup.showError(e);
			return;
		}

		UI.getCurrent().getNavigator().navigateTo(AuthenticationRealmsView.class.getSimpleName());

	}

	private void onCancel()
	{
		UI.getCurrent().getNavigator().navigateTo(AuthenticationRealmsView.class.getSimpleName());

	}

	private AuthenticationRealm getDefaultAuthenticationRealm()
	{
		AuthenticationRealm bean = new AuthenticationRealm();
		bean.setName(msg.getMessage("AuthenticationRealm.defaultName"));
		bean.setRememberMePolicy(RememberMePolicy.allowFor2ndFactor);
		bean.setAllowForRememberMeDays(14);
		bean.setBlockFor(60);
		bean.setMaxInactivity(1800);
		bean.setBlockAfterUnsuccessfulLogins(5);
		return bean;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		editor = new AuthenticationRealmEditor(msg, getDefaultAuthenticationRealm());
		main.addComponent(editor);
		Layout hl = ConfirmViewHelper.getConfirmButtonsBar(msg.getMessage("ok"),
				msg.getMessage("cancel"), () -> onConfirm(), () -> onCancel());
		main.addComponent(hl);
		setCompositionRoot(main);

	}
	
	@Override
	public String getDisplayName()
	{
		return msg.getMessage("new");
	}

	@org.springframework.stereotype.Component
	public static class NewRealmNavigationInfoProvider
			implements WebConsoleNavigationInfoProvider
	{

		private RealmsNavigationInfoProvider parent;
		private ObjectFactory<?> factory;

		@Autowired
		public NewRealmNavigationInfoProvider(RealmsNavigationInfoProvider parent,
				ObjectFactory<NewAuthenticationRealmView> factory)
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
