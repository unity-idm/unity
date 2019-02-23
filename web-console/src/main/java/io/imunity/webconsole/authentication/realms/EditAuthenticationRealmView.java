/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.realms.AuthenticationRealmsView.RealmsNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Edit realm view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class EditAuthenticationRealmView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "EditAuthenticationRealm";

	private AuthenticationRealmsController controller;
	private AuthenticationRealmEditor editor;
	private UnityMessageSource msg;
	private String realmName;

	@Autowired
	public EditAuthenticationRealmView(UnityMessageSource msg,
			AuthenticationRealmsController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		realmName = NavigationHelper.getParam(event, CommonViewParam.name.toString());
		AuthenticationRealmEntry realm;
		try
		{
			realm = controller.getRealm(realmName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			NavigationHelper.goToView(AuthenticationRealmsView.VIEW_NAME);
			return;
		}

		editor = new AuthenticationRealmEditor(msg, realm);
		editor.editMode();
	
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmEditButtonsBar(msg,
				() -> onConfirm(), () -> onCancel()));
		setCompositionRoot(main);
	}
	
	private void onConfirm()
	{
		if (editor.hasErrors())
		{
			return;
		}

		try
		{

			controller.updateRealm(editor.getAuthenticationRealm());

		} catch (

		ControllerException e)
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
		return realmName;
	}
	
	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
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
