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
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
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
 * Edit local credential view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class EditLocalCredentialView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "EditLocalCredential";

	private LocalCredentialsController controller;
	private MessageSource msg;
	private CredentialDefinitionEditor editor;
	private String credentialName;
	private EventsBus bus;

	@Autowired
	EditLocalCredentialView(MessageSource msg, LocalCredentialsController controller)
	{
		this.controller = controller;
		this.msg = msg;
		this.bus = WebSession.getCurrent().getEventBus();

	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		credentialName = NavigationHelper.getParam(event, CommonViewParam.name.toString());

		try
		{
			editor = getEditor(credentialName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(LocalCredentialsView.VIEW_NAME);
			return;
		}

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmEditButtonsBar(msg,
				() -> onConfirm(), () -> onCancel()));
		setCompositionRoot(main);
	}

	private CredentialDefinitionEditor getEditor(String toEdit) throws ControllerException
	{
		CredentialDefinition cred = controller.getCredential(toEdit);
		editor = controller.getEditor(cred);
		return editor;
	}

	private void onConfirm()
	{

		CredentialDefinition cred;
		try
		{
			cred = editor.getCredentialDefinition();
		} catch (IllegalCredentialException e1)
		{
			return;
		}

		try
		{
			controller.updateCredential(cred, editor.getLocalCredState(), bus);
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
		return credentialName;
	}

	@Component
	public static class EditLocalCredentialNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public EditLocalCredentialNavigationInfoProvider(ObjectFactory<EditLocalCredentialView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(LocalCredentialsNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}
}
