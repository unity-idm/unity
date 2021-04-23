/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.console;

import java.util.List;
import java.util.Set;

import io.imunity.upman.UpManEndpointFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent;
import pl.edu.icm.unity.webui.console.services.tabs.WebServiceAuthenticationTab;

class UpmanServiceEditor implements ServiceEditor
{
	private MessageSource msg;
	private List<String> allRealms;
	private List<AuthenticationFlowDefinition> flows;
	private List<AuthenticatorInfo> authenticators;
	private UpmanEditorComponent editor;
	private List<String> upManServices;
	private List<String> registrationForms;
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private UnityServerConfiguration serverConfig;
	private AuthenticatorSupportService authenticatorSupportService;
	private List<String> usedEndpointsPaths;
	private Set<String> serverContextPaths;
	private ImageAccessService imageAccessService;

	UpmanServiceEditor(MessageSource msg, URIAccessService uriAccessService, ImageAccessService imageAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			List<String> allRealms, List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, List<String> upManServices,
			List<String> registrationForms, List<String> usedPaths, Set<String> serverContextPaths,
			AuthenticatorSupportService authenticatorSupportService)
	{
		this.msg = msg;
		this.imageAccessService = imageAccessService;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.upManServices = upManServices;
		this.registrationForms = registrationForms;
		this.uriAccessService = uriAccessService;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.usedEndpointsPaths = usedPaths;
		this.serverContextPaths = serverContextPaths;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{

		UpmanServiceEditorGeneralTab homeServiceEditorGeneralTab = new UpmanServiceEditorGeneralTab(msg,
				UpManEndpointFactory.TYPE, usedEndpointsPaths, serverContextPaths, upManServices);
		WebServiceAuthenticationTab authenticationTab = new WebServiceAuthenticationTab(msg, uriAccessService,
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
