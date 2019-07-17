/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.home.service;

import java.util.List;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.authn.endpoints.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.endpoints.ServiceEditor;
import pl.edu.icm.unity.webui.authn.endpoints.ServiceEditorComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * 
 * @author P.Piernik
 *
 */
public class HomeServiceEditor implements ServiceEditor
{
	private UnityMessageSource msg;
	private List<String> allRealms;
	private List<AuthenticationFlowDefinition> flows;
	private List<AuthenticatorInfo> authenticators;
	private HomeServiceEditorComponent editor;
	private String extraTab;
	private List<String> allAttributes;
	private List<Group> allGroups;
	private List<String> upManServices;
	private List<String> enquiryForms;
	private List<String> registrationForms;
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private UnityServerConfiguration serverConfig;
	private AuthenticatorSupportService authenticatorSupportService;
	
	public HomeServiceEditor(UnityMessageSource msg, URIAccessService uriAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			List<String> allRealms, List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, String extraTab, List<String> allAttributes,
			List<Group> allGroups, List<String> upManServices, List<String> enquiryForms,
			List<String> registrationForms, AuthenticatorSupportService authenticatorSupportService)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.extraTab = extraTab;
		this.allAttributes = allAttributes;
		this.allGroups = allGroups;
		this.upManServices = upManServices;
		this.enquiryForms = enquiryForms;
		this.registrationForms = registrationForms;
		this.uriAccessService = uriAccessService;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		editor = new HomeServiceEditorComponent(msg, uriAccessService, fileStorageService, serverConfig,
				endpoint, allRealms, flows, authenticators, extraTab, allAttributes, allGroups,
				upManServices, enquiryForms, registrationForms, authenticatorSupportService);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServicetDefiniton();
	}
}
