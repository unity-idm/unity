/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import com.vaadin.data.Binder;

import io.imunity.scim.SCIMEndpoint;
import io.imunity.scim.config.SCIMEndpointConfigurationMapper;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.console.services.tabs.AuthenticationTab;

class SCIMServiceEditorComponent extends ServiceEditorBase
{
	private Binder<SCIMServiceConfigurationBean> restBinder;
	private Binder<DefaultServiceDefinition> serviceBinder;

	public SCIMServiceEditorComponent(MessageSource msg, SCIMServiceEditorGeneralTab generalTab,
			AuthenticationTab authTab, DefaultServiceDefinition toEdit)
	{
		super(msg);
		boolean editMode = toEdit != null;
		restBinder = new Binder<>(SCIMServiceConfigurationBean.class);
		serviceBinder = new Binder<>(DefaultServiceDefinition.class);

		generalTab.initUI(serviceBinder, restBinder, editMode);
		registerTab(generalTab);
		authTab.initUI(serviceBinder);
		registerTab(authTab);

		serviceBinder.setBean(editMode ? toEdit : new DefaultServiceDefinition(SCIMEndpoint.TYPE.getName()));
		SCIMServiceConfigurationBean config = new SCIMServiceConfigurationBean();
		if (editMode && toEdit.getConfiguration() != null)
		{
			config.setConfig(SCIMEndpointConfigurationMapper.fromProperties(toEdit.getConfiguration()));
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

		service.setConfiguration(SCIMEndpointConfigurationMapper.toProperties(restBinder.getBean().getConfig()));
		return service;
	}
}
