/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.data.binder.Binder;

import io.imunity.home.HomeEndpointProperties;
import io.imunity.home.UserHomeEndpointFactory;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceEditorBase;
import io.imunity.vaadin.auth.services.layout.ServiceWebConfiguration;
import io.imunity.vaadin.auth.services.tabs.WebServiceAuthenticationTab;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

class HomeServiceEditorComponent extends ServiceEditorBase
{
	private Binder<HomeServiceConfiguration> homeBinder;
	private Binder<DefaultServiceDefinition> serviceBinder;
	private Binder<ServiceWebConfiguration> webConfigBinder;
	private final FileStorageService fileStorageService;

	HomeServiceEditorComponent(MessageSource msg, HomeServiceEditorGeneralTab generalTab, WebServiceAuthenticationTab authTab,
			VaadinLogoImageLoader imageAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			DefaultServiceDefinition toEdit, List<Group> allGroups)
	{
		super(msg);
		this.fileStorageService = fileStorageService;
		boolean editMode = toEdit != null;
		serviceBinder = new Binder<>(DefaultServiceDefinition.class);
		homeBinder = new Binder<>(HomeServiceConfiguration.class);
		webConfigBinder = new Binder<>(ServiceWebConfiguration.class);
		
		generalTab.initUI(serviceBinder, homeBinder, editMode);
		registerTab(generalTab);
		authTab.initUI(serviceBinder, webConfigBinder);
		registerTab(authTab);
		serviceBinder.setBean(editMode ? toEdit
				: new DefaultServiceDefinition(UserHomeEndpointFactory.TYPE.getName()));
		HomeServiceConfiguration config = new HomeServiceConfiguration();
		ServiceWebConfiguration webConfig = new ServiceWebConfiguration();
		if (editMode && toEdit.getConfiguration() != null)
		{
			config.fromProperties(toEdit.getConfiguration(), msg, allGroups);
			webConfig.fromProperties(toEdit.getConfiguration(), msg, imageAccessService);
		}
		homeBinder.setBean(config);
		webConfigBinder.setBean(webConfig);
	}

	public static List<String> getAvailableTabs()
	{
		return new ArrayList<>(Arrays.asList(HomeEndpointProperties.Components.credentialTab.toString(),
				HomeEndpointProperties.Components.userDetailsTab.toString(),
				HomeEndpointProperties.Components.accountUpdateTab.toString(),
				HomeEndpointProperties.Components.trustedApplications.toString(),
				HomeEndpointProperties.Components.trustedDevices.toString()));
	}

	public static List<String> getAvailableControls()
	{
		return new ArrayList<>(Arrays.asList(
				HomeEndpointProperties.Components.attributesManagement.toString(),
				HomeEndpointProperties.Components.accountRemoval.toString(),
				HomeEndpointProperties.Components.accountLinking.toString()));
	}

	public ServiceDefinition getServiceDefiniton() throws FormValidationException
	{
		boolean hasErrors = serviceBinder.validate().hasErrors();
		hasErrors |= homeBinder.validate().hasErrors();
		hasErrors |= webConfigBinder.validate().hasErrors();
		if (hasErrors)
		{
			setErrorInTabs();
			throw new FormValidationException();
		}

		DefaultServiceDefinition service = serviceBinder.getBean();
		VaadinEndpointProperties prop = new VaadinEndpointProperties(
				webConfigBinder.getBean().toProperties(msg, fileStorageService, service.getName()));
		
		
		service.setConfiguration(homeBinder.getBean().toProperties() + "\n"
				+ prop.getAsString());
		return service;
	}

}