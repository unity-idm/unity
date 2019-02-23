/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import java.util.Collections;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.realms.AuthenticationRealmsView.RealmsNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Add realm view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class NewAuthenticationRealmView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "NewAuthenticationRealm";

	private AuthenticationRealmsController controller;
	private AuthenticationRealmEditor editor;
	private UnityMessageSource msg;

	@Autowired
	public NewAuthenticationRealmView(UnityMessageSource msg,
			AuthenticationRealmsController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		editor = new AuthenticationRealmEditor(msg, getDefaultAuthenticationRealm());	
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmNewButtonsBar(msg,
				() -> onConfirm(), () -> onCancel()));
		setCompositionRoot(main);
	}
	
	private AuthenticationRealmEntry getDefaultAuthenticationRealm()
	{
		AuthenticationRealm bean = new AuthenticationRealm();
		bean.setName(msg.getMessage("AuthenticationRealm.defaultName"));
		bean.setRememberMePolicy(RememberMePolicy.allowFor2ndFactor);
		bean.setAllowForRememberMeDays(14);
		bean.setBlockFor(60);
		bean.setMaxInactivity(1800);
		bean.setBlockAfterUnsuccessfulLogins(5);
		return new AuthenticationRealmEntry(bean, Collections.emptyList());
	}
	
	private void onConfirm()
	{
		if (editor.hasErrors())
		{
			return;
		}

		try
		{
			controller.addRealm(editor.getAuthenticationRealm());
		} catch (ControllerException e)
		{

			NotificationPopup.showError(e);
			return;
		}

		NavigationHelper.goToView(AuthenticationRealmsView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(AuthenticationRealmsView.VIEW_NAME);

	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("new");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}
	
	@Component
	public static class NewRealmNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public NewRealmNavigationInfoProvider(RealmsNavigationInfoProvider parent,
				ObjectFactory<NewAuthenticationRealmView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME,
					Type.ParameterizedView)
							.withParent(parent.getNavigationInfo())
							.withObjectFactory(factory).build());

		}
	}

}
