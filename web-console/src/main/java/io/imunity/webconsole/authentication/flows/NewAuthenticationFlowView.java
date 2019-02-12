/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.flows;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.flows.AuthenticationFlowsView.FlowsNavigationInfoProvider;
import io.imunity.webelements.helpers.ConfirmViewHelper;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Add flow view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class NewAuthenticationFlowView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "NewAuthenticationFlow";

	private AuthenticationFlowsController controller;
	private AuthenticationFlowEditor editor;
	private UnityMessageSource msg;

	@Autowired
	public NewAuthenticationFlowView(UnityMessageSource msg,
			AuthenticationFlowsController controller)
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
			if (!controller.addFlow(editor.getAuthenticationFlow()))
				return;
		} catch (ControllerException e)
		{

			NotificationPopup.showError(e);
			return;
		}

		NavigationHelper.goToView(AuthenticationFlowsView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(AuthenticationFlowsView.VIEW_NAME);

	}

	private AuthenticationFlowEntry getDefaultAuthenticationFlow()
	{
		AuthenticationFlowDefinition bean = new AuthenticationFlowDefinition();
		bean.setName(msg.getMessage("AuthenticationFlow.defaultName"));
		bean.setPolicy(Policy.REQUIRE);
		return new AuthenticationFlowEntry(bean, Collections.emptyList());
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		List<String> allAuthenticators;
		try
		{
			allAuthenticators = controller.getAllAuthenticators();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			NavigationHelper.goToView(AuthenticationFlowsView.VIEW_NAME);
			return;
		}

		editor = new AuthenticationFlowEditor(msg, getDefaultAuthenticationFlow(), allAuthenticators);
		main.addComponent(editor);
		Layout hl = ConfirmViewHelper.getConfirmButtonsBar(msg.getMessage("ok"),
				msg.getMessage("cancel"), () -> onConfirm(), () -> onCancel());
		main.addComponent(hl);
		setCompositionRoot(main);

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
	
	@org.springframework.stereotype.Component
	public static class NewRealmNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public NewRealmNavigationInfoProvider(FlowsNavigationInfoProvider parent,
				ObjectFactory<NewAuthenticationFlowView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME,
					Type.ParameterizedView)
							.withParent(parent.getNavigationInfo())
							.withObjectFactory(factory).build());

		}
	}

}
