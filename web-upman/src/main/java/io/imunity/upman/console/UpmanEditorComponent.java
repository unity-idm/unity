/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.console;


import com.vaadin.flow.data.binder.Binder;

import io.imunity.upman.UpManEndpointFactory;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceEditorBase;
import io.imunity.vaadin.auth.services.layout.ServiceWebConfiguration;
import io.imunity.vaadin.auth.services.tabs.WebServiceAuthenticationTab;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;


class UpmanEditorComponent extends ServiceEditorBase
{
	private Binder<UpmanServiceConfiguration> upmanBinder;
	private Binder<DefaultServiceDefinition> serviceBinder;
	private Binder<ServiceWebConfiguration> webConfigBinder;
	private FileStorageService fileStorageService;

	UpmanEditorComponent(MessageSource msg, UpmanServiceEditorGeneralTab generalTab,
			WebServiceAuthenticationTab authTab, VaadinLogoImageLoader imageAccessService, 
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			DefaultServiceDefinition toEdit)
	{
		super(msg);
		this.fileStorageService = fileStorageService;

		boolean editMode = toEdit != null;
		serviceBinder = new Binder<>(DefaultServiceDefinition.class);
		upmanBinder = new Binder<>(UpmanServiceConfiguration.class);
		webConfigBinder = new Binder<>(ServiceWebConfiguration.class);

		generalTab.initUI(serviceBinder, upmanBinder, editMode);
		registerTab(generalTab);
		authTab.initUI(serviceBinder, webConfigBinder);
		registerTab(authTab);
		serviceBinder.setBean(
				editMode ? toEdit : new DefaultServiceDefinition(UpManEndpointFactory.TYPE.getName()));
		UpmanServiceConfiguration config = new UpmanServiceConfiguration();
		ServiceWebConfiguration webConfig = new ServiceWebConfiguration();
		if (editMode && toEdit.getConfiguration() != null)
		{
			config.fromProperties(toEdit.getConfiguration());
			webConfig.fromProperties(toEdit.getConfiguration(), msg, imageAccessService,
					serverConfig.getValue(UnityServerConfiguration.THEME));
		}
		upmanBinder.setBean(config);
		webConfigBinder.setBean(webConfig);
	}

	public ServiceDefinition getServiceDefiniton() throws FormValidationException
	{
		boolean hasErrors = serviceBinder.validate().hasErrors();
		hasErrors |= upmanBinder.validate().hasErrors();
		hasErrors |= webConfigBinder.validate().hasErrors();
		if (hasErrors)
		{
			setErrorInTabs();
			throw new FormValidationException();
		}

		DefaultServiceDefinition service = serviceBinder.getBean();
		VaadinEndpointProperties prop = new VaadinEndpointProperties(
				webConfigBinder.getBean().toProperties(msg, fileStorageService, service.getName()));

		service.setConfiguration(upmanBinder.getBean().toProperties() + "\n" + prop.getAsString());
		return service;
	}

}