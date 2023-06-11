/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.automation;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.directorySetup.automation.AutomationView.AutomationNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * New schedule rule view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class NewAutomationView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "NewAutomation";

	private AutomationController controller;
	private MessageSource msg;
	private ScheduledRuleParamEditorImpl editor;

	NewAutomationView(AutomationController controller, MessageSource msg)
	{
		this.controller = controller;
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		try
		{
			editor = controller.getScheduleRuleEditor(null);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(AutomationView.VIEW_NAME);
			return;
		}
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmNewButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setCompositionRoot(main);
	}

	private void onConfirm()
	{
		ScheduledProcessingRuleParam rule;
		try
		{
			rule = editor.getRule();
		} catch (FormValidationException e)
		{
			return;
		}

		try
		{
			controller.scheduleRule(rule);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return;
		}

		NavigationHelper.goToView(AutomationView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(AutomationView.VIEW_NAME);

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
	public static class NewAutomationNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public NewAutomationNavigationInfoProvider(ObjectFactory<NewAutomationView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(AutomationNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}

}
