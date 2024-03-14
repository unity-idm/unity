/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest.console;


import com.vaadin.flow.data.binder.Binder;

import io.imunity.upman.rest.RESTUpmanEndpoint;
import io.imunity.upman.rest.console.UpmanRestServiceConfiguration.UpmanRestServiceConfigurationProvider;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceEditorBase;
import io.imunity.vaadin.auth.services.tabs.AuthenticationTab;
import pl.edu.icm.unity.base.message.MessageSource;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;


class UpmanRestServiceEditorComponent extends ServiceEditorBase
{
	private final Binder<UpmanRestServiceConfiguration> restBinder;
	private final Binder<DefaultServiceDefinition> serviceBinder;

	public UpmanRestServiceEditorComponent(MessageSource msg, UpmanRestServiceConfigurationProvider configProvider, UpmanRestServiceEditorGeneralTab generalTab,
	                                       AuthenticationTab authTab, DefaultServiceDefinition toEdit)
	{
		super(msg);
		boolean editMode = toEdit != null;
		restBinder = new Binder<>(UpmanRestServiceConfiguration.class);
		serviceBinder = new Binder<>(DefaultServiceDefinition.class);
		
		generalTab.initUI(serviceBinder, restBinder, editMode);
		registerTab(generalTab);
		authTab.initUI(serviceBinder);
		registerTab(authTab);
		
		serviceBinder.setBean(
				editMode ? toEdit : new DefaultServiceDefinition(RESTUpmanEndpoint.TYPE.getName()));
		UpmanRestServiceConfiguration config = configProvider.getNewConfig();
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
