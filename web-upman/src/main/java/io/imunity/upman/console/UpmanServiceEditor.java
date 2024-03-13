/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.console;

import java.util.List;
import java.util.Set;

import io.imunity.upman.UpManEndpointFactory;
import io.imunity.vaadin.endpoint.common.api.services.DefaultServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditor;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent;
import io.imunity.vaadin.endpoint.common.api.services.tabs.WebServiceAuthenticationTab;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.webui.common.FormValidationException;


class UpmanServiceEditor implements ServiceEditor
{
	private final MessageSource msg;
	private final List<String> allRealms;
	private final List<AuthenticationFlowDefinition> flows;
	private final List<AuthenticatorInfo> authenticators;
	private final List<String> upManServices;
	private final List<String> registrationForms;
	private final VaadinLogoImageLoader imageAccessService;	
	private final FileStorageService fileStorageService;
	private final UnityServerConfiguration serverConfig;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final List<String> usedEndpointsPaths;
	private final List<String> usedNames;
	private final Set<String> serverContextPaths;
	private UpmanEditorComponent editor;

	UpmanServiceEditor(MessageSource msg, VaadinLogoImageLoader imageAccessService, 
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			List<String> allRealms, List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, List<String> upManServices,
			List<String> registrationForms, List<String> usedPaths, List<String> usedNames, Set<String> serverContextPaths,
			AuthenticatorSupportService authenticatorSupportService)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.upManServices = upManServices;
		this.registrationForms = registrationForms;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.usedEndpointsPaths = usedPaths;
		this.usedNames = usedNames;
		this.serverContextPaths = serverContextPaths;
		this.imageAccessService = imageAccessService;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{

		UpmanServiceEditorGeneralTab homeServiceEditorGeneralTab = new UpmanServiceEditorGeneralTab(msg,
				UpManEndpointFactory.TYPE, usedEndpointsPaths, usedNames, serverContextPaths, upManServices);
		WebServiceAuthenticationTab authenticationTab = new WebServiceAuthenticationTab(msg,
				serverConfig, authenticatorSupportService, flows, authenticators, allRealms,
				registrationForms, UpManEndpointFactory.TYPE.getSupportedBinding());

		editor = new UpmanEditorComponent(msg, homeServiceEditorGeneralTab, authenticationTab,
				imageAccessService, fileStorageService, serverConfig,
				(DefaultServiceDefinition) endpoint);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
