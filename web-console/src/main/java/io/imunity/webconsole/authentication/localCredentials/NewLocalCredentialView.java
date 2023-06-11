/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.localCredentials;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.localCredentials.LocalCredentialsView.LocalCredentialsNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.UnityView;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * New local credential view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class NewLocalCredentialView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "NewLocalCredential";

	private LocalCredentialsController controller;
	private MessageSource msg;
	private CredentialDefinitionEditor editor;
	private EventsBus bus;

	@Autowired
	NewLocalCredentialView(MessageSource msg, LocalCredentialsController controller)
	{
		this.controller = controller;
		this.msg = msg;
		this.bus = WebSession.getCurrent().getEventBus();
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		editor = controller.getEditor(null);
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmNewButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setCompositionRoot(main);
	}

	private void onConfirm()
	{

		CredentialDefinition cred;
		try
		{
			cred = editor.getCredentialDefinition();
		} catch (IllegalCredentialException e)
		{
			return;
		}

		try
		{
			controller.addCredential(cred, bus);

		} catch (ControllerException e)
		{

			NotificationPopup.showError(msg, e);
			return;
		}

		NavigationHelper.goToView(LocalCredentialsView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(LocalCredentialsView.VIEW_NAME);

	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("new");
	}

	@Component
	public static class NewLocalCredentialNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public NewLocalCredentialNavigationInfoProvider(
				ObjectFactory<NewLocalCredentialView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(LocalCredentialsNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}
}
