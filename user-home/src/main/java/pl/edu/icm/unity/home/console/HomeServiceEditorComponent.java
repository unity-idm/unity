/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.home.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vaadin.data.Binder;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.home.HomeEndpointProperties;
import pl.edu.icm.unity.home.UserHomeEndpointFactory;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.console.services.authnlayout.ServiceWebConfiguration;
import pl.edu.icm.unity.webui.console.services.tabs.WebServiceAuthenticationTab;

class HomeServiceEditorComponent extends ServiceEditorBase
{
	private Binder<HomeServiceConfiguration> homeBinder;
	private String extraTab;
	private Binder<DefaultServiceDefinition> serviceBinder;
	private Binder<ServiceWebConfiguration> webConfigBinder;
	private FileStorageService fileStorageService;

	HomeServiceEditorComponent(MessageSource msg, HomeServiceEditorGeneralTab generalTab, WebServiceAuthenticationTab authTab,
			ImageAccessService imageAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			DefaultServiceDefinition toEdit, String extraTab, List<Group> allGroups)
	{
		super(msg);
		this.fileStorageService = fileStorageService;
		this.extraTab = extraTab;
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
			config.fromProperties(toEdit.getConfiguration(), msg, extraTab, allGroups);
			webConfig.fromProperties(toEdit.getConfiguration(), msg, imageAccessService, 
					serverConfig.getValue(UnityServerConfiguration.THEME));
		}
		homeBinder.setBean(config);
		webConfigBinder.setBean(webConfig);
	}

	public static List<String> getAvailableTabs()
	{
		return new ArrayList<String>(Arrays.asList(HomeEndpointProperties.Components.credentialTab.toString(),
				HomeEndpointProperties.Components.preferencesTab.toString(),
				HomeEndpointProperties.Components.userDetailsTab.toString(),
				HomeEndpointProperties.Components.accountUpdateTab.toString()));
	}

	public static List<String> getAvailableControls()
	{
		return new ArrayList<String>(Arrays.asList(HomeEndpointProperties.Components.userInfo.toString(),
				HomeEndpointProperties.Components.attributesManagement.toString(),
				HomeEndpointProperties.Components.identitiesManagement.toString(),
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
		
		
		service.setConfiguration(homeBinder.getBean().toProperties(extraTab) + "\n"
				+ prop.getAsString());
		return service;
	}

}