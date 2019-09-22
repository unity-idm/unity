/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;

import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.authn.services.authnlayout.ServiceWebConfiguration;
import pl.edu.icm.unity.webui.authn.services.idp.IdpEditorUsersTab;
import pl.edu.icm.unity.webui.authn.services.idp.IdpUser;
import pl.edu.icm.unity.webui.authn.services.tabs.WebServiceAuthenticationTab;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * SAML Service editor ui component. It consists of 4 tabs: general, clients,
 * users and authentication
 * 
 * @author P.Piernik
 *
 */
class SAMLServiceEditorComponent extends ServiceEditorBase
{
	private FileStorageService fileStorageService;
	private PKIManagement pkiMan;
	private Binder<SAMLServiceConfiguration> samlConfigBinder;
	private Binder<DefaultServiceDefinition> samlServiceBinder;
	private Binder<ServiceWebConfiguration> webConfigBinder;

	SAMLServiceEditorComponent(UnityMessageSource msg, EndpointTypeDescription type, PKIManagement pkiMan, SubViewSwitcher subViewSwitcher,
			NetworkServer server, URIAccessService uriAccessService, FileStorageService fileStorageService,
			UnityServerConfiguration serverConfig,
			OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory,
			ServiceDefinition toEdit, List<String> allRealms, List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, List<Group> allGroups, List<IdpUser> allUsers,
			List<String> registrationForms, Set<String> credentials, Set<String> truststores,
			AuthenticatorSupportService authenticatorSupportService, Collection<IdentityType> idTypes,
			List<String> allAttributes, List<String> usedPaths)
	{
		super(msg);
		this.fileStorageService = fileStorageService;
		this.pkiMan = pkiMan;

		boolean editMode = toEdit != null;

		samlServiceBinder = new Binder<>(DefaultServiceDefinition.class);
		samlConfigBinder = new Binder<>(SAMLServiceConfiguration.class);
		webConfigBinder = new Binder<>(ServiceWebConfiguration.class);

		registerTab(new SAMLEditorGeneralTab(msg, server, serverConfig, subViewSwitcher,
				outputTranslationProfileFieldFactory, samlServiceBinder, samlConfigBinder, editMode,
				usedPaths, credentials, truststores, idTypes));
		SAMLEditorClientsTab clientsTab = new SAMLEditorClientsTab(msg, pkiMan, serverConfig, uriAccessService,
				fileStorageService, samlConfigBinder, subViewSwitcher);
		registerTab(clientsTab);
		IdpEditorUsersTab usersTab = new SAMLUsersEditorTab(msg, samlConfigBinder, allGroups, allUsers,
				allAttributes);
		registerTab(usersTab);
		registerTab(new WebServiceAuthenticationTab(msg, uriAccessService, serverConfig,
				authenticatorSupportService, flows, authenticators, allRealms, registrationForms,
				type.getSupportedBinding(), samlServiceBinder,
				webConfigBinder, msg.getMessage("IdpServiceEditorBase.authentication")));

		DefaultServiceDefinition serviceBean = new DefaultServiceDefinition(
				type.getName());
		ServiceWebConfiguration webConfig = new ServiceWebConfiguration();
		SAMLServiceConfiguration samlConfig = new SAMLServiceConfiguration(allGroups);

		if (editMode)
		{
			serviceBean = (DefaultServiceDefinition) toEdit;
			if (serviceBean.getConfiguration() != null)
			{
				webConfig.fromProperties(serviceBean.getConfiguration(), msg, uriAccessService);
				samlConfig.fromProperties(serviceBean.getConfiguration(), msg, uriAccessService, pkiMan,
						allGroups);
			}
		}

		samlServiceBinder.setBean(serviceBean);
		samlConfigBinder.setBean(samlConfig);
		webConfigBinder.setBean(webConfig);

		if (editMode)
		{
			samlServiceBinder.validate();
			samlConfigBinder.validate();
		}

		Runnable refreshClients = () -> usersTab.setAvailableClients(clientsTab.getActiveClients().stream()
				.collect(Collectors.toMap(c -> c.getName(),
						c -> c.getDisplayedName() == null || c.getDisplayedName().isEmpty()
								? c.getName()
								: c.getDisplayedName().getValue(msg))));
		clientsTab.addClientsValueChangeListener(e -> {
			refreshClients.run();
		});
		refreshClients.run();
	}

	public ServiceDefinition getServiceDefiniton() throws FormValidationException
	{
		boolean hasErrors = samlServiceBinder.validate().hasErrors();
		hasErrors |= samlConfigBinder.validate().hasErrors();
		hasErrors |= webConfigBinder.validate().hasErrors();

		if (hasErrors)
		{
			setErrorInTabs();
			throw new FormValidationException();
		}

		DefaultServiceDefinition service = samlServiceBinder.getBean();
		VaadinEndpointProperties prop = new VaadinEndpointProperties(
				webConfigBinder.getBean().toProperties(msg, fileStorageService, service.getName()));
		try
		{
			service.setConfiguration(samlConfigBinder.getBean().toProperties(pkiMan, msg,
					fileStorageService, service.getName()) + "\n" + prop.getAsString());
		} catch (Exception e)
		{
			throw new FormValidationException("Invalid configuration of the SAML idp service", e);
		}

		return service;
	}

}
