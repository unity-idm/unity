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

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.realms.AuthenticationRealmsView.RealmsNavigationInfoProvider;
import io.imunity.webelements.helpers.ConfirmViewHelper;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * View for edit realm
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class EditAuthenticationRealmView extends CustomComponent implements UnityView
{

	public static String VIEW_NAME = "EditAuthenticationRealm";

	private AuthenticationRealmController controller;
	private AuthenticationRealmEditor editor;
	private UnityMessageSource msg;
	private String realmName;

	@Autowired
	public EditAuthenticationRealmView(UnityMessageSource msg,
			AuthenticationRealmController controller)
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
		realmName = NavigationHelper.getParam(event, "name");
		AuthenticationRealm realm;
		try
		{
			realm = controller.getRealm(realmName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			UI.getCurrent().getNavigator()
					.navigateTo(AuthenticationRealmsView.VIEW_NAME);
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
	public String getDisplayedName()
	{
		return realmName;
	}

	@org.springframework.stereotype.Component
	public static class EditRealmViewInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public EditRealmViewInfoProvider(RealmsNavigationInfoProvider parent,
				ObjectFactory<EditAuthenticationRealmView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME,
					Type.ParameterizedView)
							.withParent(parent.getNavigationInfo())
							.withObjectFactory(factory).build());

		}
	}

}
