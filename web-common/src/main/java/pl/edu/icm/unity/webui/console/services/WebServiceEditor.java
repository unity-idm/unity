package pl.edu.icm.unity.webui.console.services;

/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

import java.util.List;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent;
import pl.edu.icm.unity.webui.console.services.WebServiceEditorComponent;
import pl.edu.icm.unity.webui.console.services.tabs.GeneralTab;
import pl.edu.icm.unity.webui.console.services.tabs.WebServiceAuthenticationTab;

/**
 * Editor for services which web authentication
 * 
 * @author P.Piernik
 *
 */
public class WebServiceEditor implements ServiceEditor
{
	private UnityMessageSource msg;
	private List<String> allRealms;
	private List<AuthenticationFlowDefinition> flows;
	private List<AuthenticatorInfo> authenticators;
	private WebServiceEditorComponent editor;
	private List<String> registrationForms;
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private UnityServerConfiguration serverConfig;
	private AuthenticatorSupportService authenticatorSupportService;
	private List<String> usedPaths;
	private EndpointTypeDescription type;
	private String defaultMainTheme;

	public WebServiceEditor(EndpointTypeDescription type, UnityMessageSource msg, URIAccessService uriAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			List<String> allRealms, List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, List<String> registrationForms, List<String> usedPaths,
			AuthenticatorSupportService authenticatorSupportService, String defaultMainTheme)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.registrationForms = registrationForms;
		this.uriAccessService = uriAccessService;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.usedPaths = usedPaths;
		this.type = type;
		this.defaultMainTheme = defaultMainTheme;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{

		GeneralTab generalTab = new GeneralTab(msg, type, usedPaths);
		WebServiceAuthenticationTab webServiceAuthenticationTab = new WebServiceAuthenticationTab(msg,
				uriAccessService, serverConfig, authenticatorSupportService, flows, authenticators,
				allRealms, registrationForms, type.getSupportedBinding());

		editor = new WebServiceEditorComponent(msg, generalTab, webServiceAuthenticationTab, type,
				uriAccessService, fileStorageService, (DefaultServiceDefinition) endpoint, defaultMainTheme);

		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
