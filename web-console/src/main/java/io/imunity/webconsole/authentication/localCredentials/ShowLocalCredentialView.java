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
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Show local credential view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class ShowLocalCredentialView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "ShowLocalCredential";

	private LocalCredentialsController controller;
	private MessageSource msg;
	private String credentialName;

	@Autowired
	ShowLocalCredentialView(MessageSource msg, LocalCredentialsController controller)
	{
		this.controller = controller;
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		credentialName = NavigationHelper.getParam(event, CommonViewParam.name.toString());
		CredentialDefinitionViewer viewer;
		try
		{
			viewer = getViewer(credentialName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(LocalCredentialsView.VIEW_NAME);
			return;
		}
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(viewer);
		main.addComponent(StandardButtonsHelper
				.buildShowButtonsBar(msg, () -> onBack()));
		setCompositionRoot(main);
	}

	private CredentialDefinitionViewer getViewer(String toView) throws ControllerException
	{
		CredentialDefinition cred = controller.getCredential(toView);
		return controller.getViewer(cred);
	}

	private void onBack()
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
	public static class ShowLocalCredentialNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public ShowLocalCredentialNavigationInfoProvider(ObjectFactory<ShowLocalCredentialView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(LocalCredentialsNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}
}
