/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent;
import pl.edu.icm.unity.webui.console.services.idp.IdpEditorUsersTab;
import pl.edu.icm.unity.webui.console.services.idp.IdpUser;
import pl.edu.icm.unity.webui.console.services.idp.PolicyAgreementsTab;
import pl.edu.icm.unity.webui.console.services.tabs.WebServiceAuthenticationTab;

class OAuthServiceEditor implements ServiceEditor
{
	private MessageSource msg;
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
	private ImageAccessService imageService;
	private FileStorageService fileStorageService;
	private UnityServerConfiguration serverConfig;
	private AuthenticatorSupportService authenticatorSupportService;
	private String serverPrefix;
	private Set<String> serverContextPaths;
	private Collection<IdentityType> idTypes;
	private SubViewSwitcher subViewSwitcher;
	private OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory;
	private List<String> usedPaths;
	private List<String> allUsernames;
	private URIAccessService uriAccessService;
	private Collection<PolicyDocumentWithRevision> policyDocuments;

	OAuthServiceEditor(MessageSource msg, 
			SubViewSwitcher subViewSwitcher,
			OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory, 
			String serverPrefix,
			Set<String> serverContextPaths,
			ImageAccessService imageService, 
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
			List<String> usedPaths,
			Collection<PolicyDocumentWithRevision> policyDocuments)
	{
		this.msg = msg;
		this.uriAccessService = uriAccessService;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.allAttributes = allAttributes;
		this.allGroups = allGroups;
		this.registrationForms = registrationForms;
		this.imageService = imageService;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.credentials = credentials;
		this.serverPrefix = serverPrefix;
		this.serverContextPaths = serverContextPaths;
		this.idTypes = idTypes;
		this.subViewSwitcher = subViewSwitcher;
		this.outputTranslationProfileFieldFactory = outputTranslationProfileFieldFactory;
		this.usedPaths = usedPaths;
		this.allIdpUsers = allIdpUsers;
		this.systemClientsSupplier = systemClientsSupplier;
		this.allUsernames = allUsernames;
		this.policyDocuments = policyDocuments;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		OAuthEditorGeneralTab generalTab = new OAuthEditorGeneralTab(msg, serverPrefix, serverContextPaths, subViewSwitcher,
				outputTranslationProfileFieldFactory, 
				endpoint != null, credentials, idTypes, allAttributes, usedPaths);
		OAuthEditorClientsTab clientsTab = new OAuthEditorClientsTab(msg, serverConfig, uriAccessService,
				subViewSwitcher, flows, authenticators, allRealms, allUsernames, generalTab::getScopes,
				OAuthTokenEndpoint.TYPE.getSupportedBinding());
		WebServiceAuthenticationTab webAuthTab = new WebServiceAuthenticationTab(msg, uriAccessService, serverConfig,
				authenticatorSupportService, flows, authenticators, allRealms, registrationForms,
				OAuthAuthzWebEndpoint.Factory.TYPE.getSupportedBinding(),
				msg.getMessage("IdpServiceEditorBase.authentication"));
		IdpEditorUsersTab usersTab = new IdpEditorUsersTab(msg, allGroups, allIdpUsers,
				allAttributes);
		
		PolicyAgreementsTab policyAgreementTab = new PolicyAgreementsTab(msg, policyDocuments);
		
		editor = new OAuthServiceEditorComponent(msg, generalTab, clientsTab, usersTab, webAuthTab, policyAgreementTab,
				fileStorageService, imageService, endpoint, allGroups, systemClientsSupplier, 
				serverConfig.getValue(UnityServerConfiguration.THEME));
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
