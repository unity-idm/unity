/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.authenticators;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.ViewWithSubViewBase;
import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.setup.AuthenticationSetupView;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Edit authenticator view
 * @author P.Piernik
 *
 */
@PrototypeComponent
class EditAuthenticatorView extends ViewWithSubViewBase
{
	public static final String VIEW_NAME = "EditAuthenticator";

	private AuthenticatorsController controller;
	private MainAuthenticatorEditor editor;
	private UnityMessageSource msg;
	private String authenticatorName;
	private VerticalLayout mainView;

	@Autowired
	EditAuthenticatorView(UnityMessageSource msg, AuthenticatorsController controller)
	{
		super();
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		authenticatorName = NavigationHelper.getParam(event, CommonViewParam.name.toString());
		AuthenticatorEntry authenticator;
		try
		{
			authenticator = controller.getAuthenticator(authenticatorName);
			
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			NavigationHelper.goToView(AuthenticationSetupView.VIEW_NAME);
			return;
		}
		
		editor = controller.getEditor(authenticator, this);
		mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(editor);
		mainView.addComponent(StandardButtonsHelper.buildConfirmEditButtonsBar(msg,
				() -> onConfirm(), () -> onCancel()));
		setMainView(mainView);
	}

	private void onConfirm()
	{
		
		AuthenticatorDefinition authenticator;
		try
		{
			authenticator = editor.getAuthenticator();
		} catch (FormValidationException e1)
		{
			return;
		}
		
		try
		{
			controller.updateAuthenticator(authenticator);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}

		NavigationHelper.goToView(AuthenticationSetupView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(AuthenticationSetupView.VIEW_NAME);

	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public String getDisplayedName()
	{
		return authenticatorName;
	}

	@Component
	public static class EditAuthenticatorViewInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public EditAuthenticatorViewInfoProvider(AuthenticatorsNavigationInfoProvider parent,
				ObjectFactory<EditAuthenticatorView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedViewWithSubviews)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory).build());

		}
	}
}
