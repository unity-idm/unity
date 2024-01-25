/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.services.base;

import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;

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

public abstract class NewServiceViewBase extends ConsoleViewComponent
{
	private final ServiceControllerBase controller;
	private final NotificationPresenter notificationPresenter;
	private final Class<? extends ConsoleViewComponent> mainServicesViewName;
	private final MessageSource msg;
	private BreadCrumbParameter breadCrumbParameter;
	private MainServiceEditor editor;
	
	
	public NewServiceViewBase(MessageSource msg, ServiceControllerBase controller,
			Class<? extends ConsoleViewComponent> mainServicesViewName, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.mainServicesViewName = mainServicesViewName;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String param)
	{
		breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("new"));

		
		try
		{
			editor = controller.getEditor(null, ServiceEditorTab.GENERAL, createSubViewSwitcher());
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getMessage());
			UI.getCurrent().navigate(mainServicesViewName);
			return;
		}
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.add(editor);
		main.add(EditViewActionLayoutFactory.createActionLayout(msg, false, mainServicesViewName, () -> onConfirm()));
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
			controller.deploy(service);
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
