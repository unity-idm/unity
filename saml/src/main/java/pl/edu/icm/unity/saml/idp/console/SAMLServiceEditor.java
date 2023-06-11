/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
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

/**
 * SAML Service editor
 * 
 * @author P.Piernik
 *
 */
public class SAMLServiceEditor implements ServiceEditor
{
	private MessageSource msg;
	private EndpointTypeDescription type;
	private PKIManagement pkiMan;
	private List<String> allRealms;
	private List<AuthenticationFlowDefinition> flows;
	private List<AuthenticatorInfo> authenticators;
	private SAMLServiceEditorComponent editor;
	private List<String> allAttributes;
	private List<Group> allGroups;
	private List<IdpUser> allUsers;
	private List<String> registrationForms;
	private Set<String> credentials;
	private Set<String> truststores;
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private UnityServerConfiguration serverConfig;
	private AuthenticatorSupportService authenticatorSupportService;
	private String serverPrefix;
	private Set<String> serverContextPaths;
	private Collection<IdentityType> idTypes;
	private SubViewSwitcher subViewSwitcher;
	private OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory;
	private List<String> usedPaths;
	private ImageAccessService imageAccessService;
	private Collection<PolicyDocumentWithRevision> policyDocuments;

	public SAMLServiceEditor(MessageSource msg, EndpointTypeDescription type, PKIManagement pkiMan,
			SubViewSwitcher subViewSwitcher,
			OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory,
			String serverPrefix,
			Set<String> serverContextPaths,
			URIAccessService uriAccessService, 
			ImageAccessService imageAccessService, FileStorageService fileStorageService,
			UnityServerConfiguration serverConfig, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,
			List<String> allAttributes, List<Group> allGroups, List<IdpUser> allUsers,
			List<String> registrationForms, Set<String> credentials, Set<String> truststores,
			AuthenticatorSupportService authenticatorSupportService, Collection<IdentityType> idTypes,
			List<String> usedPaths, Collection<PolicyDocumentWithRevision> policyDocuments)
	{
		this.msg = msg;
		this.type = type;
		this.imageAccessService = imageAccessService;
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
		this.idTypes = idTypes;
		this.subViewSwitcher = subViewSwitcher;
		this.outputTranslationProfileFieldFactory = outputTranslationProfileFieldFactory;
		this.usedPaths = usedPaths;
		this.allUsers = allUsers;
		this.truststores = truststores;
		this.pkiMan = pkiMan;
		this.serverPrefix = serverPrefix;
		this.serverContextPaths = serverContextPaths;
		this.policyDocuments = policyDocuments;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		SAMLEditorGeneralTab samlEditorGeneralTab = new SAMLEditorGeneralTab(msg, serverPrefix, serverContextPaths, serverConfig, subViewSwitcher,
				outputTranslationProfileFieldFactory,
				usedPaths, credentials, truststores, idTypes);
		
		SAMLEditorClientsTab clientsTab = new SAMLEditorClientsTab(msg, pkiMan, serverConfig, uriAccessService,
				fileStorageService, subViewSwitcher);
		
		IdpEditorUsersTab usersTab = new SAMLUsersEditorTab(msg, allGroups, allUsers,
				allAttributes);
		
		WebServiceAuthenticationTab webServiceAuthenticationTab = new WebServiceAuthenticationTab(msg, uriAccessService, serverConfig,
				authenticatorSupportService, flows, authenticators, allRealms, registrationForms,
				type.getSupportedBinding(), msg.getMessage("IdpServiceEditorBase.authentication"));
		
		PolicyAgreementsTab policyAgreementTab = new PolicyAgreementsTab(msg, policyDocuments);
		
		editor = new SAMLServiceEditorComponent(msg, samlEditorGeneralTab, clientsTab, usersTab, webServiceAuthenticationTab,
				policyAgreementTab, type, pkiMan, uriAccessService, imageAccessService, fileStorageService, endpoint, allGroups, 
				serverConfig.getValue(UnityServerConfiguration.THEME));
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
