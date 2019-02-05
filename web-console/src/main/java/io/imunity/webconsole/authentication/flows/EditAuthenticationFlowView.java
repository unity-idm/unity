/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.flows;

import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.flows.AuthenticationFlowsView.FlowsNavigationInfoProvider;
import io.imunity.webelements.helpers.ConfirmViewHelper;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Edit flow view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class EditAuthenticationFlowView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "EditAuthenticationFlow";

	private AuthenticationFlowsController controller;
	private AuthenticationFlowEditor editor;
	private UnityMessageSource msg;
	private String flowName;

	@Autowired
	public EditAuthenticationFlowView(UnityMessageSource msg,
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
			if (!controller.updateFlow(editor.getAuthenticationFlow()))
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

	@Override
	public void enter(ViewChangeEvent event)
	{
		flowName = NavigationHelper.getParam(event, CommonViewParam.name.toString());
		AuthenticationFlowEntry flow;
		try
		{
			flow = controller.getFlow(flowName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			NavigationHelper.goToView(AuthenticationFlowsView.VIEW_NAME);
			return;
		}
		
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

		editor = new AuthenticationFlowEditor(msg, flow, allAuthenticators);
		editor.editMode();
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.setWidth(44, Unit.EM);
		Layout hl = ConfirmViewHelper.getConfirmButtonsBar(msg.getMessage("save"),
				msg.getMessage("close"), () -> onConfirm(), () -> onCancel());
		main.addComponent(hl);
		main.setComponentAlignment(hl, Alignment.BOTTOM_CENTER);
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{
		return flowName;
	}
	
	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@org.springframework.stereotype.Component
	public static class EditFlowViewInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public EditFlowViewInfoProvider(FlowsNavigationInfoProvider parent,
				ObjectFactory<EditAuthenticationFlowView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME,
					Type.ParameterizedView)
							.withParent(parent.getNavigationInfo())
							.withObjectFactory(factory).build());

		}
	}

}
