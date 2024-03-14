/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.auth.services.layout.ServiceWebConfiguration;
import io.imunity.vaadin.auth.services.tabs.GeneralTab;
import io.imunity.vaadin.auth.services.tabs.WebServiceAuthenticationTab;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.files.FileStorageService;

/**
 * Service editor component with web authentication configuration support. It
 * consists of two tabs: general and authentication.
 * 
 * @author P.Piernik
 *
 */
public class WebServiceEditorComponent extends ServiceEditorBase
{
	private final FileStorageService fileStorageService;
	private Binder<DefaultServiceDefinition> serviceBinder;
	private Binder<ServiceWebConfiguration> webConfigBinder;
	
	public WebServiceEditorComponent(MessageSource msg, GeneralTab generalTab, 
			WebServiceAuthenticationTab authTab,  EndpointTypeDescription type,
			VaadinLogoImageLoader imageAccessService, FileStorageService fileStorageService,  
			DefaultServiceDefinition toEdit, String defaultMainTheme)
	{
		super(msg);
		boolean editMode = toEdit != null;
		this.fileStorageService = fileStorageService;
		
		serviceBinder = new Binder<>(DefaultServiceDefinition.class);
		webConfigBinder = new Binder<>(ServiceWebConfiguration.class);
		generalTab.initUI(serviceBinder, editMode);
		registerTab(generalTab);
		authTab.initUI(serviceBinder, webConfigBinder);
		registerTab(authTab);
		DefaultServiceDefinition service = new DefaultServiceDefinition(type.getName());
		ServiceWebConfiguration webConfig = new ServiceWebConfiguration(defaultMainTheme);
		if (editMode)
		{
			service = (DefaultServiceDefinition) toEdit;
			if (service.getConfiguration() != null)
				webConfig.fromProperties(service.getConfiguration(), msg, imageAccessService, null);
		}
		serviceBinder.setBean(service);
		webConfigBinder.setBean(webConfig);
	}

	public ServiceDefinition getServiceDefiniton() throws FormValidationException
	{
		boolean hasErrors = serviceBinder.validate().hasErrors();
		hasErrors |= webConfigBinder.validate().hasErrors();

		if (hasErrors)
		{
			setErrorInTabs();
			throw new FormValidationException();
		}

		DefaultServiceDefinition service = serviceBinder.getBean();
		VaadinEndpointProperties prop = new VaadinEndpointProperties(
				webConfigBinder.getBean().toProperties(msg, fileStorageService, service.getName()));
		service.setConfiguration(prop.getAsString());
		return service;
	}

	@Override
	public Component getComponent()
	{
		return this;
	}
}
