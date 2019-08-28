/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;

import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import pl.edu.icm.unity.oauth.service.OAuthClient.OAuthClientsBean;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.authn.services.authnlayout.ServiceWebConfiguration;
import pl.edu.icm.unity.webui.authn.services.idp.IdpEditorUsersTab;
import pl.edu.icm.unity.webui.authn.services.idp.IdpUser;
import pl.edu.icm.unity.webui.authn.services.tabs.WebServiceAuthenticationTab;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.groups.GroupWithIndentIndicator;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * 
 * @author P.Piernik
 *
 */
class OAuthServiceEditorComponent extends ServiceEditorBase
{
	public static final String TOKEN_SERVICE_NAME_SUFFIX = "_TOKEN";

	private Binder<DefaultServiceDefinition> oauthServiceWebAuthzBinder;
	private Binder<DefaultServiceDefinition> oauthServiceTokenBinder;;
	private Binder<OAuthServiceConfiguration> oauthConfigBinder;
	private Binder<ServiceWebConfiguration> webConfigBinder;
	private Binder<OAuthClientsBean> clientsBinder;
	private FileStorageService fileStorageService;
	private Group autoGenGroup;
	private boolean editMode;

	OAuthServiceEditorComponent(UnityMessageSource msg, SubViewSwitcher subViewSwitcher, NetworkServer server,
			URIAccessService uriAccessService, FileStorageService fileStorageService,
			UnityServerConfiguration serverConfig, OutputTranslationProfileFieldFactory profileFieldFactory,
			ServiceDefinition toEdit, List<String> allRealms, List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, List<Group> allGroups, List<IdpUser> allUsers,
			List<OAuthClient> allClients, List<String> registrationForms, Set<String> credentials,
			AuthenticatorSupportService authenticatorSupportService, Collection<IdentityType> idTypes,
			List<String> attrTypes, List<String> usedPaths)
	{
		super(msg);
		editMode = toEdit != null;

		this.fileStorageService = fileStorageService;

		oauthServiceWebAuthzBinder = new Binder<>(DefaultServiceDefinition.class);
		oauthConfigBinder = new Binder<>(OAuthServiceConfiguration.class);
		oauthServiceTokenBinder = new Binder<>(DefaultServiceDefinition.class);
		webConfigBinder = new Binder<>(ServiceWebConfiguration.class);
		clientsBinder = new Binder<>(OAuthClientsBean.class);

		List<Group> groupsWithAutoGen = new ArrayList<>();
		groupsWithAutoGen.addAll(allGroups);

		autoGenGroup = new Group(
				OAuthServiceController.IDP_CLIENT_MAIN_GROUP + "/" + UUID.randomUUID().toString());
		if (!editMode)
		{
			groupsWithAutoGen.add(autoGenGroup);
		}

		OAuthEditorGeneralTab generalTab = new OAuthEditorGeneralTab(msg, server, subViewSwitcher,
				profileFieldFactory, oauthServiceWebAuthzBinder, oauthServiceTokenBinder,
				oauthConfigBinder, editMode, credentials, idTypes, attrTypes, usedPaths);

		OAuthEditorClientsTab clientsTab = new OAuthEditorClientsTab(msg, serverConfig, uriAccessService,
				subViewSwitcher, flows, authenticators, allRealms, groupsWithAutoGen, allUsers,
				OAuthTokenEndpoint.TYPE.getSupportedBinding(), oauthServiceTokenBinder,
				oauthConfigBinder, clientsBinder);

		IdpEditorUsersTab usersTab = new IdpEditorUsersTab(msg, oauthConfigBinder, allGroups, allUsers,
				attrTypes);

		generalTab.addNameValueChangeListener(e -> {
			if (e.getValue() != null && !e.getValue().isEmpty())
			{
				autoGenGroup.setDisplayedName(new I18nString(e.getValue() + "/oauth-client"));
			} else
			{
				autoGenGroup.setDisplayedName(new I18nString(autoGenGroup.toString()));
			}

			clientsTab.refreshGroups();
		});

		registerTab(generalTab);
		registerTab(clientsTab);
		registerTab(usersTab);
		registerTab(new WebServiceAuthenticationTab(msg, uriAccessService, serverConfig,
				authenticatorSupportService, flows, authenticators, allRealms, registrationForms,
				OAuthAuthzWebEndpoint.Factory.TYPE.getSupportedBinding(), oauthServiceWebAuthzBinder,
				webConfigBinder, msg.getMessage("IdpServiceEditorBase.authentication")));

		OAuthServiceDefinition oauthServiceToEdit;
		OAuthServiceConfiguration oauthConfig = new OAuthServiceConfiguration(allGroups);
		oauthConfig.setClientGroup(new GroupWithIndentIndicator(autoGenGroup, false));

		DefaultServiceDefinition webAuthzService = new DefaultServiceDefinition(
				OAuthAuthzWebEndpoint.Factory.TYPE.getName());
		DefaultServiceDefinition tokenService = new DefaultServiceDefinition(OAuthTokenEndpoint.TYPE.getName());
		ServiceWebConfiguration webConfig = new ServiceWebConfiguration();
		OAuthClientsBean clientsBean = new OAuthClientsBean();
		clientsBean.setClients(cloneClients(allClients));

		if (editMode)
		{
			oauthServiceToEdit = (OAuthServiceDefinition) toEdit;
			webAuthzService = oauthServiceToEdit.getWebAuthzService();
			tokenService = oauthServiceToEdit.getTokenService();

			if (webAuthzService != null && webAuthzService.getConfiguration() != null)
			{
				oauthConfig.fromProperties(webAuthzService.getConfiguration(), msg, allGroups);
				webConfig.fromProperties(webAuthzService.getConfiguration(), msg, uriAccessService);
			}

		}

		oauthConfigBinder.setBean(oauthConfig);
		clientsBinder.setBean(clientsBean);
		oauthServiceWebAuthzBinder.setBean(webAuthzService);
		oauthServiceTokenBinder.setBean(tokenService);
		webConfigBinder.setBean(webConfig);

		if (editMode)
		{
			oauthServiceWebAuthzBinder.validate();
			oauthServiceTokenBinder.validate();
		}

		Runnable refreshClients = () -> usersTab.setAvailableClients(clientsTab.getActiveClients().stream()
				.collect(Collectors.toMap(c -> c.getId(),
						c -> c.getName() == null || c.getName().isEmpty() ? c.getId()
								: c.getName())));
		clientsBinder.addValueChangeListener(e -> refreshClients.run());
		clientsTab.addGroupValueChangeListener(e -> {
			clientsBean.setClients(cloneClients(allClients));
			clientsBinder.setBean(clientsBean);
			refreshClients.run();
		});
		refreshClients.run();
	}

	private List<OAuthClient> cloneClients(List<OAuthClient> clients)
	{
		List<OAuthClient> clone = new ArrayList<>();
		for (OAuthClient c : clients)
		{
			clone.add(c.clone());
		}
		return clone;
	}

	public ServiceDefinition getServiceDefiniton() throws FormValidationException
	{
		boolean hasErrors = oauthServiceWebAuthzBinder.validate().hasErrors();
		hasErrors |= oauthConfigBinder.validate().hasErrors();
		hasErrors |= oauthServiceTokenBinder.validate().hasErrors();
		hasErrors |= webConfigBinder.validate().hasErrors();

		if (hasErrors)
		{
			setErrorInTabs();
			throw new FormValidationException();
		}

		DefaultServiceDefinition webAuthz = oauthServiceWebAuthzBinder.getBean();
		VaadinEndpointProperties prop = new VaadinEndpointProperties(
				webConfigBinder.getBean().toProperties(msg, fileStorageService, webAuthz.getName()));
		webAuthz.setConfiguration(oauthConfigBinder.getBean().toProperties() + "\n" + prop.getAsString());

		DefaultServiceDefinition token = oauthServiceTokenBinder.getBean();
		token.setConfiguration(oauthConfigBinder.getBean().toProperties());

		if (token.getName() == null || token.getName().isEmpty())
		{
			token.setName(webAuthz.getName() + TOKEN_SERVICE_NAME_SUFFIX);
		}
		OAuthServiceDefinition def = new OAuthServiceDefinition(webAuthz, token);
		def.setClients(clientsBinder.getBean().getClients());
		if (!editMode)
		{
			def.setAutoGeneratedClientsGroup(autoGenGroup.toString());
		}
		return def;
	}

}