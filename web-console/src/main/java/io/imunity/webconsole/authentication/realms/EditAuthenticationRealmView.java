/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Layout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProvider;
import io.imunity.webconsole.authentication.realms.AuthenticationRealmsView.RealmsNavigationInfoProvider;
import io.imunity.webelements.helpers.ConfirmViewHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityViewBase;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.ControllerException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * View for edit realm
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class EditAuthenticationRealmView extends UnityViewBase
{

	public static String VIEW_NAME = "EditAuthenticationRealm";

	private AuthenticationRealmController controller;
	private AuthenticationRealmEditor editor;
	private UnityMessageSource msg;
	private String realmName;

	@Autowired
	public EditAuthenticationRealmView(UnityMessageSource msg, AuthenticationRealmController controller)
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
			if (!controller.updateRealm(editor.getAuthenticationRealm()))
				return;
		} catch (ControllerException e)
		{

			NotificationPopup.showError(e);
			return;
		}

		UI.getCurrent().getNavigator().navigateTo(AuthenticationRealmsView.VIEW_NAME);

	}

	private void onCancel()
	{
		UI.getCurrent().getNavigator().navigateTo(AuthenticationRealmsView.VIEW_NAME);

	}

	@Override
	public void enter(ViewChangeEvent event)
	{
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

		editor = new AuthenticationRealmEditor(msg, realm);
		VerticalLayout main = new VerticalLayout();

		main.addComponent(editor);
		Layout hl = ConfirmViewHelper.getConfirmButtonsBar(msg.getMessage("ok"),
				msg.getMessage("cancel"), () -> onConfirm(), () -> onCancel());
		main.addComponent(hl);
		setCompositionRoot(main);
	}
	
	@Override
	public String getDisplayName()
	{
		return realmName;
	}

	@org.springframework.stereotype.Component
	public static class EditRealmViewInfoProvider implements WebConsoleNavigationInfoProvider
	{

		private RealmsNavigationInfoProvider parent;
		private ObjectFactory<?> factory;

		@Autowired
		public EditRealmViewInfoProvider(RealmsNavigationInfoProvider parent,
				ObjectFactory<EditAuthenticationRealmView> factory)
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
