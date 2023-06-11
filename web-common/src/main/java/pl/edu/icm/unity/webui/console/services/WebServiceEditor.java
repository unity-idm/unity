package pl.edu.icm.unity.webui.console.services;

/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
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
	private MessageSource msg;
	private List<String> allRealms;
	private List<AuthenticationFlowDefinition> flows;
	private List<AuthenticatorInfo> authenticators;
	private WebServiceEditorComponent editor;
	private List<String> registrationForms;
	private ImageAccessService imageAccessService;
	private FileStorageService fileStorageService;
	private UnityServerConfiguration serverConfig;
	private AuthenticatorSupportService authenticatorSupportService;
	private List<String> usedPaths;
	private Set<String> serverContextPaths;
	private EndpointTypeDescription type;
	private String defaultMainTheme;
	private final URIAccessService uriAccessService;

	public WebServiceEditor(EndpointTypeDescription type, MessageSource msg,
			URIAccessService uriAccessService,
			ImageAccessService imageAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			List<String> allRealms, List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, List<String> registrationForms, List<String> usedPaths, Set<String> serverContextPaths,
			AuthenticatorSupportService authenticatorSupportService, String defaultMainTheme)
	{
		this.msg = msg;
		this.uriAccessService = uriAccessService;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.registrationForms = registrationForms;
		this.imageAccessService = imageAccessService;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.usedPaths = usedPaths;
		this.serverContextPaths = serverContextPaths;
		this.type = type;
		this.defaultMainTheme = defaultMainTheme;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{

		GeneralTab generalTab = new GeneralTab(msg, type, usedPaths, serverContextPaths);
		WebServiceAuthenticationTab webServiceAuthenticationTab = new WebServiceAuthenticationTab(msg,
				uriAccessService, serverConfig, authenticatorSupportService, flows, authenticators,
				allRealms, registrationForms, type.getSupportedBinding());

		editor = new WebServiceEditorComponent(msg, generalTab, webServiceAuthenticationTab, type,
				imageAccessService, fileStorageService, (DefaultServiceDefinition) endpoint, defaultMainTheme);

		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
