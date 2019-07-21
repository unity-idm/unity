/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.services;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.services.ServicesView.ServicesNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.authn.services.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class EditServiceView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "EditService";

	private UnityMessageSource msg;
	private ServicesController controller;
	private MainServiceEditor editor;
	private String serviceName;

	@Autowired
	EditServiceView(UnityMessageSource msg, ServicesController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{

		serviceName = NavigationHelper.getParam(event, CommonViewParam.name.toString());
		String tabParam = NavigationHelper.getParam(event, CommonViewParam.tab.toString());
		
		ServiceEditorTab activeTab = tabParam != null && tabParam.equals(ServiceEditorTab.GENERAL.toString().toLowerCase()) ? ServiceEditorTab.GENERAL : ServiceEditorTab.AUTHENTICATION;
		
		
		ServiceDefinition service;
		try
		{
			service = controller.getService(serviceName);

		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(ServicesView.VIEW_NAME);
			return;
		}

		try
		{
			editor = controller.getEditor(service, activeTab);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(ServicesView.VIEW_NAME);
			return;
		}
		
		
		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(editor);
		mainView.addComponent(StandardButtonsHelper.buildConfirmEditButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setCompositionRoot(mainView);
	}

	private void onConfirm()
	{

		ServiceDefinition service;
		try
		{
			service = editor.getService();
		} catch (FormValidationException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("EditServiceView.invalidConfiguration"),
					e);
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

		NavigationHelper.goToView(ServicesView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(ServicesView.VIEW_NAME);

	}

	@Override
	public String getDisplayedName()
	{
		return serviceName;
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class EditServiceNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public EditServiceNavigationInfoProvider(ServicesNavigationInfoProvider parent,
				ObjectFactory<EditServiceView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory).build());

		}
	}
}
