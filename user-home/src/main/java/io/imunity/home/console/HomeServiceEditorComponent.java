/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.console;

import com.vaadin.data.Binder;
import io.imunity.home.HomeEndpointProperties;
import io.imunity.home.UserHomeEndpointFactory;
import pl.edu.icm.unity.base.group.Group;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class HomeServiceEditorComponent extends ServiceEditorBase
{
	private Binder<HomeServiceConfiguration> homeBinder;
	private Binder<DefaultServiceDefinition> serviceBinder;
	private Binder<ServiceWebConfiguration> webConfigBinder;
	private FileStorageService fileStorageService;

	HomeServiceEditorComponent(MessageSource msg, HomeServiceEditorGeneralTab generalTab, WebServiceAuthenticationTab authTab,
			ImageAccessService imageAccessService,
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
			webConfig.fromProperties(toEdit.getConfiguration(), msg, imageAccessService, 
					serverConfig.getValue(UnityServerConfiguration.THEME));
		}
		homeBinder.setBean(config);
		webConfigBinder.setBean(webConfig);
	}

	public static List<String> getAvailableTabs()
	{
		return new ArrayList<String>(Arrays.asList(HomeEndpointProperties.Components.credentialTab.toString(),
				HomeEndpointProperties.Components.userDetailsTab.toString(),
				HomeEndpointProperties.Components.accountUpdateTab.toString(),
				HomeEndpointProperties.Components.trustedApplications.toString()));
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