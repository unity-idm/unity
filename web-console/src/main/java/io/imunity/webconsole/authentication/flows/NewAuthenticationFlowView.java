/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.flows;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.facilities.AuthenticationFacilitiesView;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Add flow view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class NewAuthenticationFlowView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "NewAuthenticationFlow";

	private AuthenticationFlowsController controller;
	private AuthenticationFlowEditor editor;
	private MessageSource msg;

	@Autowired
	NewAuthenticationFlowView(MessageSource msg,
			AuthenticationFlowsController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	private AuthenticationFlowEntry getDefaultAuthenticationFlow()
	{
		AuthenticationFlowDefinitionForBinder bean = new AuthenticationFlowDefinitionForBinder(
				msg.getMessage("AuthenticationFlow.defaultName"), Policy.REQUIRE, Set.of(), Collections.emptyList(),
				VIEW_NAME);
		return new AuthenticationFlowEntry(bean, Collections.emptyList());
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		List<String> allAuthenticators;
		try
		{
			allAuthenticators = controller.getAllAuthenticators();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(AuthenticationFacilitiesView.VIEW_NAME);
			return;
		}

		editor = new AuthenticationFlowEditor(msg, getDefaultAuthenticationFlow(), allAuthenticators);
		
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmNewButtonsBar(msg,
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
			controller.addFlow(editor.getAuthenticationFlow());
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
	public static class NewAuthenticationFlowNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public NewAuthenticationFlowNavigationInfoProvider(ObjectFactory<NewAuthenticationFlowView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME,
					Type.ParameterizedView)
							.withParent(AuthenticationFlowsNavigationInfoProvider.ID)
							.withObjectFactory(factory).build());

		}
	}

}
