/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.services;

import java.util.List;

import com.vaadin.data.Binder;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.services.authnlayout.ServiceWebConfiguration;
import pl.edu.icm.unity.webui.authn.services.tabs.GeneralTab;
import pl.edu.icm.unity.webui.authn.services.tabs.WebServiceAuthenticationTab;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * 
 * @author P.Piernik
 *
 */
public class WebServiceEditorComponent extends ServiceEditorBase
{
	private Binder<DefaultServiceDefinition> serviceBinder;
	private Binder<ServiceWebConfiguration> webConfigBinder;
	private FileStorageService fileStorageService;

	public WebServiceEditorComponent(UnityMessageSource msg, EndpointTypeDescription type,
			URIAccessService uriAccessService, FileStorageService fileStorageService,
			UnityServerConfiguration serverConfig, DefaultServiceDefinition toEdit, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,
			List<String> registrationForms, List<String> usedPaths, AuthenticatorSupportService authenticatorSupportService)
	{
		super(msg);
		boolean editMode = toEdit != null;

		serviceBinder = new Binder<>(DefaultServiceDefinition.class);
		webConfigBinder = new Binder<>(ServiceWebConfiguration.class);

		registerTab(new GeneralTab(msg, serviceBinder, type, usedPaths, editMode));
		registerTab(new WebServiceAuthenticationTab(msg, uriAccessService, serverConfig,
				authenticatorSupportService, flows, authenticators, allRealms, registrationForms,
				type.getSupportedBinding(), serviceBinder, webConfigBinder));

		DefaultServiceDefinition service = new DefaultServiceDefinition(type.getName());
		ServiceWebConfiguration webConfig = new ServiceWebConfiguration();

		if (editMode)
		{
			service = (DefaultServiceDefinition) toEdit;

			if (service.getConfiguration() != null)
			{
				webConfig.fromProperties(service.getConfiguration(), msg, uriAccessService);
			}
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
