/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.console;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent;
import pl.edu.icm.unity.webui.console.services.idp.IdpUser;

class OAuthServiceEditor implements ServiceEditor
{
	private UnityMessageSource msg;
	private List<String> allRealms;
	private List<AuthenticationFlowDefinition> flows;
	private List<AuthenticatorInfo> authenticators;
	private OAuthServiceEditorComponent editor;
	private List<String> allAttributes;
	private List<Group> allGroups;
	private List<IdpUser> allIdpUsers;
	private Function<String, List<OAuthClient>> systemClientsSupplier;
	private List<String> registrationForms;
	private Set<String> credentials;
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private UnityServerConfiguration serverConfig;
	private AuthenticatorSupportService authenticatorSupportService;
	private NetworkServer server;
	private Collection<IdentityType> idTypes;
	private SubViewSwitcher subViewSwitcher;
	private OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory;
	private List<String> usedPaths;
	private List<String> allUsernames;

	OAuthServiceEditor(UnityMessageSource msg, 
			SubViewSwitcher subViewSwitcher,
			OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory, 
			NetworkServer server,
			URIAccessService uriAccessService, 
			FileStorageService fileStorageService,
			UnityServerConfiguration serverConfig, 
			List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, 
			List<AuthenticatorInfo> authenticators,
			List<String> allAttributes, 
			List<Group> allGroups, 
			List<IdpUser> allIdpUsers,
			Function<String, List<OAuthClient>> systemClientsSupplier, 
			List<String> allUsernames, 
			List<String> registrationForms, 
			Set<String> credentials,
			AuthenticatorSupportService authenticatorSupportService, 
			Collection<IdentityType> idTypes,
			List<String> usedPaths)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.allAttributes = allAttributes;
		this.allGroups = allGroups;
		this.registrationForms = registrationForms;
		this.uriAccessService = uriAccessService;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.credentials = credentials;
		this.server = server;
		this.idTypes = idTypes;
		this.subViewSwitcher = subViewSwitcher;
		this.outputTranslationProfileFieldFactory = outputTranslationProfileFieldFactory;
		this.usedPaths = usedPaths;
		this.allIdpUsers = allIdpUsers;
		this.systemClientsSupplier = systemClientsSupplier;
		this.allUsernames = allUsernames;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		OAuthEditorGeneralTab generalTab = new OAuthEditorGeneralTab(msg, server, subViewSwitcher,
				outputTranslationProfileFieldFactory, 
				endpoint != null, credentials, idTypes, allAttributes, usedPaths);
		OAuthEditorClientsTab clientsTab = new OAuthEditorClientsTab(msg, serverConfig, uriAccessService,
				subViewSwitcher, flows, authenticators, allRealms, allUsernames,
				OAuthTokenEndpoint.TYPE.getSupportedBinding());
		editor = new OAuthServiceEditorComponent(msg, generalTab, clientsTab, uriAccessService,
				fileStorageService, serverConfig, endpoint,
				allRealms, flows, authenticators, allGroups, allIdpUsers, systemClientsSupplier, registrationForms,
				authenticatorSupportService, allAttributes);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
