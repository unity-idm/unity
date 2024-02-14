/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.services.base;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import io.imunity.console.components.InfoBanner;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.EditViewActionLayoutFactory;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.api.services.ServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent.ServiceEditorTab;
import io.imunity.vaadin.endpoint.common.sub_view_switcher.DefaultSubViewSwitcher;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.Optional;

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
	private VerticalLayout mainView;
	private VerticalLayout unsavedInfoBanner;



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
		getContent().removeAll();
		mainView = new VerticalLayout();
		unsavedInfoBanner = new InfoBanner(msg::getMessage);
		try
		{
			editor = controller.getEditor(null, ServiceEditorTab.GENERAL, createSubViewSwitcher());
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getMessage());
			UI.getCurrent().navigate(mainServicesViewName);
			return;
		}
		mainView.setMargin(false);
		mainView.add(editor);
		mainView.add(EditViewActionLayoutFactory.createActionLayout(msg, false, mainServicesViewName, this::onConfirm));
		getContent().add(unsavedInfoBanner);
		getContent().add(mainView);
	}

	private void onConfirm()
	{

		ServiceDefinition service;
		try
		{
			service = editor.getService();
		} catch (FormValidationException e)
		{
			String description = e.getMessage() == null ? msg.getMessage("Generic.formErrorHint") : e.getMessage();
			notificationPresenter.showError(msg.getMessage("NewServiceView.invalidConfiguration"), description);
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

	private SubViewSwitcher createSubViewSwitcher()
	{
		return new DefaultSubViewSwitcher(this, mainView, unsavedInfoBanner, breadCrumbParameter, this::setBreadCrumbParameter);
	}

	private void setBreadCrumbParameter(BreadCrumbParameter parameter)
	{
		breadCrumbParameter = parameter;
	}
}
