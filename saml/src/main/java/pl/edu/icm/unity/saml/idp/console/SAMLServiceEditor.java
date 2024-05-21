/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import io.imunity.console.utils.tprofile.OutputTranslationProfileFieldFactory;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.auth.services.ServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceEditor;
import io.imunity.vaadin.auth.services.ServiceEditorComponent;
import io.imunity.vaadin.auth.services.idp.IdpEditorUsersTab;
import io.imunity.vaadin.auth.services.idp.IdpUser;
import io.imunity.vaadin.auth.services.idp.PolicyAgreementsTab;
import io.imunity.vaadin.auth.services.tabs.WebServiceAuthenticationTab;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
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
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * SAML Service editor
 * 
 * @author P.Piernik
 *
 */
public class SAMLServiceEditor implements ServiceEditor
{
	private final MessageSource msg;
	private final EndpointTypeDescription type;
	private final PKIManagement pkiMan;
	private final List<String> allRealms;
	private final List<AuthenticationFlowDefinition> flows;
	private final List<AuthenticatorInfo> authenticators;
	private SAMLServiceEditorComponent editor;
	private final List<String> allAttributes;
	private final List<Group> allGroups;
	private final List<IdpUser> allUsers;
	private final List<String> registrationForms;
	private final Set<String> credentials;
	private final Set<String> truststores;
	private final URIAccessService uriAccessService;
	private final FileStorageService fileStorageService;
	private final UnityServerConfiguration serverConfig;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final String serverPrefix;
	private final Set<String> serverContextPaths;
	private final Collection<IdentityType> idTypes;
	private final SubViewSwitcher subViewSwitcher;
	private final OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory;
	private final List<String> usedPaths;
	private final VaadinLogoImageLoader imageAccessService;
	private final Collection<PolicyDocumentWithRevision> policyDocuments;
	private final NotificationPresenter notificationPresenter;

	public SAMLServiceEditor(MessageSource msg, EndpointTypeDescription type, PKIManagement pkiMan,
			SubViewSwitcher subViewSwitcher,
			OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory,
			String serverPrefix,
			Set<String> serverContextPaths,
			URIAccessService uriAccessService,
			VaadinLogoImageLoader imageAccessService, FileStorageService fileStorageService,
			UnityServerConfiguration serverConfig, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,
			List<String> allAttributes, List<Group> allGroups, List<IdpUser> allUsers,
			List<String> registrationForms, Set<String> credentials, Set<String> truststores,
			AuthenticatorSupportService authenticatorSupportService, Collection<IdentityType> idTypes,
			List<String> usedPaths, Collection<PolicyDocumentWithRevision> policyDocuments,
			NotificationPresenter notificationPresenter)
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
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		SAMLEditorGeneralTab samlEditorGeneralTab = new SAMLEditorGeneralTab(msg, serverPrefix, serverContextPaths, serverConfig, subViewSwitcher,
				outputTranslationProfileFieldFactory,
				usedPaths, credentials, truststores, idTypes);
		
		SAMLEditorClientsTab clientsTab = new SAMLEditorClientsTab(msg, pkiMan, serverConfig, uriAccessService,
				fileStorageService, subViewSwitcher, notificationPresenter);
		
		IdpEditorUsersTab usersTab = new SAMLUsersEditorTab(msg, allGroups, allUsers,
				allAttributes);
		
		WebServiceAuthenticationTab webServiceAuthenticationTab = new WebServiceAuthenticationTab(msg, serverConfig,
				authenticatorSupportService, flows, authenticators, allRealms, registrationForms,
				type.getSupportedBinding(), msg.getMessage("IdpServiceEditorBase.authentication"));
		
		PolicyAgreementsTab policyAgreementTab = new PolicyAgreementsTab(msg, policyDocuments);
		
		editor = new SAMLServiceEditorComponent(msg, samlEditorGeneralTab, clientsTab, usersTab, webServiceAuthenticationTab,
				policyAgreementTab, type, pkiMan, uriAccessService, imageAccessService, fileStorageService, endpoint, allGroups);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
