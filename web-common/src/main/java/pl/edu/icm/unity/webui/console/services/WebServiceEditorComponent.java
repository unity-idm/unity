/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services;

import com.vaadin.data.Binder;

import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.console.services.authnlayout.ServiceWebConfiguration;
import pl.edu.icm.unity.webui.console.services.tabs.GeneralTab;
import pl.edu.icm.unity.webui.console.services.tabs.WebServiceAuthenticationTab;

/**
 * Service editor component with web authentication configuration support. It
 * consists of two tabs: general and authentication.
 * 
 * @author P.Piernik
 *
 */
public class WebServiceEditorComponent extends ServiceEditorBase
{
	private Binder<DefaultServiceDefinition> serviceBinder;
	private Binder<ServiceWebConfiguration> webConfigBinder;
	private FileStorageService fileStorageService;

	public WebServiceEditorComponent(MessageSource msg, GeneralTab generalTab, 
			WebServiceAuthenticationTab authTab,  EndpointTypeDescription type,
			ImageAccessService imageAccessService, FileStorageService fileStorageService,  
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
}
