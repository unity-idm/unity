/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceEditorBase;
import io.imunity.vaadin.auth.services.layout.ServiceWebConfiguration;
import io.imunity.vaadin.auth.services.idp.GroupWithIndentIndicator;
import io.imunity.vaadin.auth.services.idp.IdpEditorUsersTab;
import io.imunity.vaadin.auth.services.idp.PolicyAgreementsTab;
import io.imunity.vaadin.auth.services.tabs.WebServiceAuthenticationTab;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import org.apache.commons.lang3.RandomStringUtils;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.oauth.as.console.OAuthClient.OAuthClientsBean;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static pl.edu.icm.unity.oauth.as.console.OAuthServiceController.IDP_CLIENT_MAIN_GROUP;
import static pl.edu.icm.unity.oauth.as.console.OAuthServiceController.OAUTH_CLIENTS_SUBGROUP;

class OAuthServiceEditorComponent extends ServiceEditorBase
{
	public static final String TOKEN_SERVICE_NAME_SUFFIX = "_TOKEN";

	private final Binder<DefaultServiceDefinition> oauthServiceWebAuthzBinder;
	private final Binder<DefaultServiceDefinition> oauthServiceTokenBinder;
	private final Binder<OAuthServiceConfiguration> oauthConfigBinder;
	private final Binder<ServiceWebConfiguration> webConfigBinder;
	private final Binder<OAuthClientsBean> clientsBinder;
	private final FileStorageService fileStorageService;
	private final Group generatedIdPGroup;
	private final boolean editMode;

	OAuthServiceEditorComponent(MessageSource msg, OAuthEditorGeneralTab generalTab, OAuthEditorClientsTab clientsTab,
			IdpEditorUsersTab usersTab, WebServiceAuthenticationTab webAuthTab, PolicyAgreementsTab policyAgreementTab,
			FileStorageService fileStorageService, VaadinLogoImageLoader imageAccessService,
			OAuthScopesService scopeService, ServiceDefinition toEdit,
			List<Group> allGroups, Function<String, List<OAuthClient>> systemClientsSupplier)
	{
		super(msg);
		this.fileStorageService = fileStorageService;
		editMode = toEdit != null;
		oauthServiceWebAuthzBinder = new Binder<>(DefaultServiceDefinition.class);
		oauthConfigBinder = new Binder<>(OAuthServiceConfiguration.class);
		oauthServiceTokenBinder = new Binder<>(DefaultServiceDefinition.class);
		webConfigBinder = new Binder<>(ServiceWebConfiguration.class);
		clientsBinder = new Binder<>(OAuthClientsBean.class);

		List<Group> groupsWithAutoGen = new ArrayList<>();
		groupsWithAutoGen.addAll(allGroups);
		generatedIdPGroup = generateRandomIdPGroup(allGroups);
		Group generatedClientsGroup = new Group(generatedIdPGroup, OAUTH_CLIENTS_SUBGROUP);
		generatedClientsGroup.setDisplayedName(new I18nString(OAUTH_CLIENTS_SUBGROUP));

		if (!editMode)
		{
			if (!allGroups.stream().map(Group::toString).anyMatch(g -> g.equals(IDP_CLIENT_MAIN_GROUP)))
			{
				groupsWithAutoGen.add(new Group(IDP_CLIENT_MAIN_GROUP));
			}

			groupsWithAutoGen.add(generatedIdPGroup);
			groupsWithAutoGen.add(generatedClientsGroup);
		}

		generalTab.initUI(oauthServiceWebAuthzBinder, oauthServiceTokenBinder, oauthConfigBinder);
		clientsTab.initUI(groupsWithAutoGen, oauthServiceTokenBinder, oauthConfigBinder, clientsBinder);
		usersTab.initUI(oauthConfigBinder);

		generalTab.addNameValueChangeListener(value -> {
			String displayedName = (value != null && !value.isEmpty()) ? value
					: generatedIdPGroup.toString();
			generatedIdPGroup.setDisplayedName(new I18nString(displayedName));
			clientsTab.refreshGroups();
		});
		webAuthTab.initUI(oauthServiceWebAuthzBinder, webConfigBinder);

		oauthConfigBinder.forField(policyAgreementTab).asRequired().bind("policyAgreementConfig");
		
		registerTab(generalTab);
		registerTab(clientsTab);
		registerTab(usersTab);
		registerTab(webAuthTab);
		registerTab(policyAgreementTab);

		OAuthServiceDefinition oauthServiceToEdit;
		OAuthServiceConfiguration oauthConfig = new OAuthServiceConfiguration(msg, allGroups, scopeService);
		oauthConfig.setClientGroup(new GroupWithIndentIndicator(generatedClientsGroup, false));

		DefaultServiceDefinition webAuthzService = new DefaultServiceDefinition(
				OAuthAuthzWebEndpoint.Factory.TYPE.getName());
		DefaultServiceDefinition tokenService = new DefaultServiceDefinition(OAuthTokenEndpoint.TYPE.getName());
		ServiceWebConfiguration webConfig = new ServiceWebConfiguration();
		OAuthClientsBean clientsBean = new OAuthClientsBean();

		if (editMode)
		{
			oauthServiceToEdit = (OAuthServiceDefinition) toEdit;
			webAuthzService = oauthServiceToEdit.getWebAuthzService();
			tokenService = oauthServiceToEdit.getTokenService();

			if (webAuthzService != null && webAuthzService.getConfiguration() != null)
			{
				oauthConfig.fromProperties(msg, webAuthzService.getConfiguration(), allGroups, scopeService);
				webConfig.fromProperties(webAuthzService.getConfiguration(), msg, imageAccessService);
			}
			clientsBean.setClients(cloneClients(
					systemClientsSupplier.apply(oauthConfig.getClientGroup().group().toString())));
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
				.collect(Collectors.toMap(OAuthClient::getId,
						c -> c.getName() == null || c.getName().isEmpty() ? c.getId()
								: c.getName())));
		clientsBinder.addValueChangeListener(e -> refreshClients.run());
		clientsTab.addGroupValueChangeListener(value -> {
			Group newGroup = value.group();
			List<OAuthClient> newGroupClients = (newGroup.equals(generatedClientsGroup)
					|| newGroup.equals(generatedIdPGroup)) ? Collections.emptyList()
							: cloneClients(systemClientsSupplier
									.apply(newGroup.toString()));
			clientsBean.setClients(newGroupClients);
			clientsBinder.setBean(clientsBean);
			refreshClients.run();
		});
		refreshClients.run();
	}

	private Group generateRandomIdPGroup(List<Group> allGroups)
	{
		String genPath = null;
		do
		{
			genPath = OAuthServiceController.IDP_CLIENT_MAIN_GROUP + "/"
					+ RandomStringUtils.randomAlphabetic(6).toLowerCase();
		} while (checkIfGroupExists(allGroups, genPath));

		return new Group(genPath);
	}

	private boolean checkIfGroupExists(List<Group> allGroups, String path)
	{
		return allGroups.stream().filter(group -> group.toString().equals(path)).findAny().isPresent();
	}

	private List<OAuthClient> cloneClients(List<OAuthClient> clients)
	{
		return clients.stream().map(OAuthClient::clone).collect(Collectors.toList());
	}

	ServiceDefinition getServiceDefiniton() throws FormValidationException
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
		webAuthz.setConfiguration(oauthConfigBinder.getBean().toProperties(msg) + "\n" + prop.getAsString());

		DefaultServiceDefinition token = oauthServiceTokenBinder.getBean();
		token.setConfiguration(oauthConfigBinder.getBean().toProperties(msg));

		if (token.getName() == null || token.getName().isEmpty())
		{
			token.setName(webAuthz.getName() + TOKEN_SERVICE_NAME_SUFFIX);
		}
		OAuthServiceDefinition def = new OAuthServiceDefinition(webAuthz, token);
		def.setSelectedClients(clientsBinder.getBean().getClients());
		if (!editMode)
		{
			def.setAutoGeneratedClientsGroup(generatedIdPGroup.toString());
		}
		return def;
	}

}