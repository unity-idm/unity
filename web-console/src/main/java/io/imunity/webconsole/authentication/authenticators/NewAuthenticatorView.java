/**
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
 * View for add new authenticator
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class NewAuthenticatorView extends ViewWithSubViewBase
{
	public static final String VIEW_NAME = "NewAuthenticator";

	private UnityMessageSource msg;
	private AuthenticatorsController controller;
	private MainAuthenticatorEditor editor;

	private VerticalLayout mainView;

	private String displayedName;
	
	@Autowired
	NewAuthenticatorView(UnityMessageSource msg, AuthenticatorsController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		editor = controller.getEditor(null, this, e -> {
			displayedName = msg.getMessage("New") + " " + AuthenticatorTypeLabelHelper
					.getAuthenticatorTypeLabel(msg, e.getValue()).toLowerCase();
			refreshBreadCrumbs();
		});
		mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(editor);
		mainView.addComponent(StandardButtonsHelper.buildConfirmNewButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setMainView(mainView);
		refreshBreadCrumbs();
	}

	private void onConfirm()
	{

		AuthenticatorDefinition authenticator;
		try
		{
			authenticator = editor.getAuthenticator();
		} catch (FormValidationException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("NewAuthenticatorView.invalidConfiguration"),
					e);
			return;
		}

		try
		{
			controller.addAuthenticator(authenticator);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return;
		}

		NavigationHelper.goToView(AuthenticationSetupView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(AuthenticationSetupView.VIEW_NAME);

	}

	@Override
	public String getDisplayedName()
	{
		return displayedName;
	}
		
	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class NewAuthenticatorNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public NewAuthenticatorNavigationInfoProvider(AuthenticatorsNavigationInfoProvider parent,
				ObjectFactory<NewAuthenticatorView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedViewWithSubviews)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory).build());

		}
	}
}
