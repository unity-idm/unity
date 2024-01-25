/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.services.base;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;

import io.imunity.console.views.CommonViewParam;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.EditViewActionLayoutFactory;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.api.services.ServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */

public abstract class EditServiceViewBase extends ConsoleViewComponent
{
	private final ServiceControllerBase controller;
	private final Class<? extends ConsoleViewComponent> mainServicesViewName;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private BreadCrumbParameter breadCrumbParameter;
	private MainServiceEditor editor;

	
	public EditServiceViewBase(MessageSource msg, ServiceControllerBase controller,
		 Class<? extends ConsoleViewComponent> mainServicesViewName, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.mainServicesViewName = mainServicesViewName;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String serviceName)
	{
		breadCrumbParameter = new BreadCrumbParameter(serviceName, serviceName);
		Location location = event.getLocation();
		Map<String, List<String>> queryParameters = location.getQueryParameters()
				.getParameters();

		String tabParam = Optional.ofNullable(queryParameters.getOrDefault(CommonViewParam.tab.name(), null))
				.map(l -> l.get(0))
				.orElse(null);
		
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
			notificationPresenter.showError(e.getCaption(), e.getMessage());
			UI.getCurrent().navigate(mainServicesViewName);
			return;
		}

		try
		{
			editor = controller.getEditor(service, activeTab, createSubViewSwitcher());
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getMessage());
			UI.getCurrent().navigate(mainServicesViewName);
			return;
		}

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.add(editor);
		main.add(EditViewActionLayoutFactory.createActionLayout(msg, true, mainServicesViewName, () -> onConfirm()));
		getContent().add(main);
	}

	private void onConfirm()
	{

		ServiceDefinition service;
		try
		{
			service = editor.getService();
		} catch (FormValidationException e)
		{
			notificationPresenter.showError(msg.getMessage("NewServiceView.invalidConfiguration"), e.getMessage());
			return;
		}

		try
		{
			controller.update(service);
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getMessage());
			return;
		}

		UI.getCurrent().navigate(mainServicesViewName);

	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}


	//TODO
	private SubViewSwitcher createSubViewSwitcher()
	{
		return null;
	}
}
