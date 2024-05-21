/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services;

import io.imunity.vaadin.auth.services.tabs.GeneralTab;
import io.imunity.vaadin.auth.services.tabs.WebServiceAuthenticationTab;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;

import java.util.List;
import java.util.Set;


/**
 * Editor for services which web authentication
 * 
 * @author P.Piernik
 *
 */
public class WebServiceEditor implements ServiceEditor
{
	private final MessageSource msg;
	private final List<String> allRealms;
	private final List<AuthenticationFlowDefinition> flows;
	private final List<AuthenticatorInfo> authenticators;
	private final List<String> registrationForms;
	private final VaadinLogoImageLoader imageAccessService;
	private final FileStorageService fileStorageService;
	private final UnityServerConfiguration serverConfig;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final List<String> usedPaths;
	private final List<String> usedNames;
	private final Set<String> serverContextPaths;
	private final EndpointTypeDescription type;
	private  WebServiceEditorComponent editor;

	public WebServiceEditor(EndpointTypeDescription type, MessageSource msg,
			
			VaadinLogoImageLoader imageAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			List<String> allRealms, List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, List<String> registrationForms, List<String> usedPaths, List<String> usedNames,
			Set<String> serverContextPaths,
			AuthenticatorSupportService authenticatorSupportService)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.registrationForms = registrationForms;
		this.imageAccessService = imageAccessService;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.usedPaths = usedPaths;
		this.usedNames = usedNames;
		this.serverContextPaths = serverContextPaths;
		this.type = type;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{

		GeneralTab generalTab = new GeneralTab(msg, type, usedPaths, usedNames, serverContextPaths);
		WebServiceAuthenticationTab webServiceAuthenticationTab = new WebServiceAuthenticationTab(msg,
				serverConfig, authenticatorSupportService, flows, authenticators,
				allRealms, registrationForms, type.getSupportedBinding());

		editor = new WebServiceEditorComponent(msg, generalTab, webServiceAuthenticationTab, type,
				imageAccessService, fileStorageService, (DefaultServiceDefinition) endpoint);

		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
