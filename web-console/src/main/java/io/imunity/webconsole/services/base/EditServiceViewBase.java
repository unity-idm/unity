/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.services.base;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.ViewWithSubViewBase;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */

public abstract class EditServiceViewBase extends ViewWithSubViewBase
{
	private ServiceControllerBase controller;
	private MainServiceEditor editor;
	private String mainServicesViewName;
	private String serviceName;

	public EditServiceViewBase(MessageSource msg, ServiceControllerBase controller,
			String mainServicesViewName)
	{
		super(msg);
		this.controller = controller;
		this.mainServicesViewName = mainServicesViewName;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{

		serviceName = NavigationHelper.getParam(event, CommonViewParam.name.toString());
		String tabParam = NavigationHelper.getParam(event, CommonViewParam.tab.toString());

		ServiceEditorTab activeTab;
		try
		{
			activeTab = tabParam != null ? ServiceEditorTab.valueOf(tabParam.toUpperCase())
					: ServiceEditorTab.GENERAL;
		} catch (Exception e)
		{
			activeTab = ServiceEditorTab.GENERAL;
		}

		ServiceDefinition service;
		try
		{
			service = controller.getService(serviceName);

		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(mainServicesViewName);
			return;
		}

		try
		{
			editor = controller.getEditor(service, activeTab, this);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(mainServicesViewName);
			return;
		}

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(editor);
		mainView.addComponent(StandardButtonsHelper.buildConfirmEditButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setMainView(mainView);
		refreshBreadCrumbs();
	}

	private void onConfirm()
	{

		ServiceDefinition service;
		try
		{
			service = editor.getService();
		} catch (FormValidationException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("EditServiceView.invalidConfiguration"), e);
			return;
		}

		try
		{
			controller.update(service);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return;
		}

		NavigationHelper.goToView(mainServicesViewName);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(mainServicesViewName);

	}

	@Override
	public String getDisplayedName()
	{
		return serviceName;
	}

	@Override
	public abstract String getViewName();

}
