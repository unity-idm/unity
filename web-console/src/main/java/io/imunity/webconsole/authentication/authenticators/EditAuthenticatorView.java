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

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.facilities.AuthenticationFacilitiesView;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.ViewWithSubViewBase;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Edit authenticator view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class EditAuthenticatorView extends ViewWithSubViewBase
{
	public static final String VIEW_NAME = "EditAuthenticator";

	private AuthenticatorsController controller;
	private MainAuthenticatorEditor editor;
	private MessageSource msg;
	private String authenticatorName;
	private VerticalLayout mainView;

	@Autowired
	EditAuthenticatorView(MessageSource msg, AuthenticatorsController controller)
	{
		super(msg);
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
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(AuthenticationFacilitiesView.VIEW_NAME);
			return;
		}

		editor = controller.getEditor(authenticator, this, null);
		mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(editor);
		mainView.addComponent(StandardButtonsHelper.buildConfirmEditButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		refreshBreadCrumbs();
		setMainView(mainView);
	}

	private void onConfirm()
	{

		AuthenticatorDefinition authenticator;
		try
		{
			authenticator = editor.getAuthenticator();
		} catch (FormValidationException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("EditAuthenticatorView.invalidConfiguration"),
					e);
			return;
		}

		try
		{
			controller.updateAuthenticator(authenticator);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return;
		}

		NavigationHelper.goToView(AuthenticationFacilitiesView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(AuthenticationFacilitiesView.VIEW_NAME);

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
		public EditAuthenticatorViewInfoProvider(ObjectFactory<EditAuthenticatorView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedViewWithSubviews)
					.withParent(AuthenticatorsNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}
}
