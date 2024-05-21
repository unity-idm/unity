/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

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
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

class OAuthServiceEditor implements ServiceEditor
{
	private final MessageSource msg;
	private final List<String> allRealms;
	private final List<AuthenticationFlowDefinition> flows;
	private final List<AuthenticatorInfo> authenticators;
	private OAuthServiceEditorComponent editor;
	private final List<String> allAttributes;
	private final List<Group> allGroups;
	private final List<IdpUser> allIdpUsers;
	private final Function<String, List<OAuthClient>> systemClientsSupplier;
	private final List<String> registrationForms;
	private final Set<String> credentials;
	private final VaadinLogoImageLoader imageService;
	private final FileStorageService fileStorageService;
	private final UnityServerConfiguration serverConfig;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final String serverPrefix;
	private final Set<String> serverContextPaths;
	private final Collection<IdentityType> idTypes;
	private final SubViewSwitcher subViewSwitcher;
	private final OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory;
	private final List<String> usedPaths;
	private final List<String> allUsernames;
	private final NotificationPresenter notificationPresenter;
	private final Collection<PolicyDocumentWithRevision> policyDocuments;
	private final OAuthScopesService scopeService;
	private final Set<String> validators;
	private final Set<String> certificates;

	OAuthServiceEditor(MessageSource msg, 
			SubViewSwitcher subViewSwitcher,
			OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory,
			String serverPrefix,
			Set<String> serverContextPaths,
			VaadinLogoImageLoader imageService,
			NotificationPresenter notificationPresenter,
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
			Collection<PolicyDocumentWithRevision> policyDocuments,
			OAuthScopesService scopeService,	
			Set<String> validators,
			Set<String> certificates)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
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
		this.scopeService = scopeService;
		this.validators = validators;
		this.certificates = certificates;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		OAuthEditorGeneralTab generalTab = new OAuthEditorGeneralTab(msg, serverPrefix, serverContextPaths,
				subViewSwitcher, outputTranslationProfileFieldFactory, endpoint != null, credentials, idTypes,
				allAttributes, usedPaths, scopeService.getSystemScopes(), validators, certificates);
		OAuthEditorClientsTab clientsTab = new OAuthEditorClientsTab(msg, serverConfig,
				subViewSwitcher, flows, authenticators, allRealms, allUsernames, generalTab::getScopes,
				OAuthTokenEndpoint.TYPE.getSupportedBinding(), notificationPresenter);
		WebServiceAuthenticationTab webAuthTab = new WebServiceAuthenticationTab(msg, serverConfig,
				authenticatorSupportService, flows, authenticators, allRealms, registrationForms,
				OAuthAuthzWebEndpoint.Factory.TYPE.getSupportedBinding(),
				msg.getMessage("IdpServiceEditorBase.authentication"));
		IdpEditorUsersTab usersTab = new IdpEditorUsersTab(msg, allGroups, allIdpUsers,
				allAttributes);
		
		PolicyAgreementsTab policyAgreementTab = new PolicyAgreementsTab(msg, policyDocuments);
		
		editor = new OAuthServiceEditorComponent(msg, generalTab, clientsTab, usersTab, webAuthTab, policyAgreementTab,
				fileStorageService, imageService, scopeService, endpoint, allGroups, systemClientsSupplier);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
