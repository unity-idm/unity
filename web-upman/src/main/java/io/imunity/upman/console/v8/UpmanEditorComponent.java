/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.console.v8;

import com.vaadin.data.Binder;

import io.imunity.upman.UpManEndpointFactory;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.console.services.authnlayout.ServiceWebConfiguration;
import pl.edu.icm.unity.webui.console.services.tabs.WebServiceAuthenticationTab;

class UpmanEditorComponent extends ServiceEditorBase
{
	private Binder<UpmanServiceConfiguration> upmanBinder;
	private Binder<DefaultServiceDefinition> serviceBinder;
	private Binder<ServiceWebConfiguration> webConfigBinder;
	private FileStorageService fileStorageService;

	UpmanEditorComponent(MessageSource msg, UpmanServiceEditorGeneralTab generalTab,
			WebServiceAuthenticationTab authTab, ImageAccessService imageAccessService,
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