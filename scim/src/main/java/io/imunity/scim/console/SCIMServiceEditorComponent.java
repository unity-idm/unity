/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.scim.SCIMEndpoint;
import io.imunity.scim.config.SCIMEndpointPropertiesConfigurationMapper;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceEditorBase;
import io.imunity.vaadin.auth.services.tabs.AuthenticationTab;
import pl.edu.icm.unity.base.message.MessageSource;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;


class SCIMServiceEditorComponent extends ServiceEditorBase
{
	private Binder<SCIMServiceConfigurationBean> restBinder;
	private Binder<DefaultServiceDefinition> serviceBinder;
	private final ConfigurationVaadinBeanMapper configurationVaadinBeanMapper;

	public SCIMServiceEditorComponent(MessageSource msg, ConfigurationVaadinBeanMapper configurationVaadinBeanMapper, SCIMServiceEditorGeneralTab generalTab,
			AuthenticationTab authTab, SCIMServiceEditorSchemaTab schemaTab, DefaultServiceDefinition toEdit)
	{
		super(msg);
		this.configurationVaadinBeanMapper = configurationVaadinBeanMapper;
		
		boolean editMode = toEdit != null;
		restBinder = new Binder<>(SCIMServiceConfigurationBean.class);
		serviceBinder = new Binder<>(DefaultServiceDefinition.class);		
		generalTab.initUI(serviceBinder, restBinder, editMode);
		registerTab(generalTab);
		authTab.initUI(serviceBinder);
		registerTab(authTab);
		schemaTab.initUI(restBinder);
		registerTab(schemaTab);

		serviceBinder.setBean(editMode ? toEdit : new DefaultServiceDefinition(SCIMEndpoint.TYPE.getName()));
		SCIMServiceConfigurationBean config = new SCIMServiceConfigurationBean(configurationVaadinBeanMapper);
		if (editMode && toEdit.getConfiguration() != null)
		{
			config = configurationVaadinBeanMapper
					.mapToBean(SCIMEndpointPropertiesConfigurationMapper.fromProperties(toEdit.getConfiguration()));
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

		try
		{
			service.setConfiguration(SCIMEndpointPropertiesConfigurationMapper
					.toProperties(configurationVaadinBeanMapper.mapToConfigurationBean(restBinder.getBean())));
		} catch (JsonProcessingException e)
		{
			setErrorInTabs();
			throw new FormValidationException();
		}
		return service;
	}
}
