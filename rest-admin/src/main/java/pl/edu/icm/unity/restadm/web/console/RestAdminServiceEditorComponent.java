/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.web.console;

import com.vaadin.data.Binder;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.restadm.RESTAdminEndpoint;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.console.services.tabs.AuthenticationTab;

/**
 * Rest admin service editor component
 * 
 * @author P.Piernik
 *
 */
class RestAdminServiceEditorComponent extends ServiceEditorBase
{
	private Binder<RestAdminServiceConfiguration> restBinder;
	private Binder<DefaultServiceDefinition> serviceBinder;

	public RestAdminServiceEditorComponent(MessageSource msg, RestAdminServiceEditorGeneralTab generalTab,
			AuthenticationTab authTab, DefaultServiceDefinition toEdit)
	{
		super(msg);
		boolean editMode = toEdit != null;
		restBinder = new Binder<>(RestAdminServiceConfiguration.class);
		serviceBinder = new Binder<>(DefaultServiceDefinition.class);
		
		generalTab.initUI(serviceBinder, restBinder, editMode);
		registerTab(generalTab);
		authTab.initUI(serviceBinder);
		registerTab(authTab);
		
		serviceBinder.setBean(
				editMode ? toEdit : new DefaultServiceDefinition(RESTAdminEndpoint.TYPE.getName()));
		RestAdminServiceConfiguration config = new RestAdminServiceConfiguration();
		if (editMode && toEdit.getConfiguration() != null)
		{
			config.fromProperties(toEdit.getConfiguration(), msg);
		}
		restBinder.setBean(config);
	}

	ServiceDefinition getServiceDefiniton() throws FormValidationException
	{
		boolean hasErrors = serviceBinder.validate().hasErrors();
		hasErrors |= restBinder.validate().hasErrors();
		if (hasErrors)
		{
			setErrorInTabs();
			throw new FormValidationException();
		}

		DefaultServiceDefinition service = serviceBinder.getBean();
		service.setConfiguration(restBinder.getBean().toProperties());
		return service;
	}
}
