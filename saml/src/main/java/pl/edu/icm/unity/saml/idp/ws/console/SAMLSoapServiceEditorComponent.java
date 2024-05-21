/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.ws.console;

import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceEditorBase;
import io.imunity.vaadin.auth.services.idp.IdpEditorUsersTab;
import io.imunity.vaadin.auth.services.tabs.AuthenticationTab;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.saml.idp.console.SAMLEditorClientsTab;
import pl.edu.icm.unity.saml.idp.console.SAMLEditorGeneralTab;
import pl.edu.icm.unity.saml.idp.console.SAMLIndividualTrustedSPConfiguration;
import pl.edu.icm.unity.saml.idp.console.SAMLServiceConfiguration;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SAML SAOP Service editor ui component. It consists of 4 tabs: general,
 * clients, users and authentication
 * 
 * @author P.Piernik
 *
 */
class SAMLSoapServiceEditorComponent extends ServiceEditorBase
{
	private final FileStorageService fileStorageService;
	private final PKIManagement pkiMan;
	private final Binder<SAMLServiceConfiguration> samlConfigBinder;
	private final Binder<DefaultServiceDefinition> samlServiceBinder;

	SAMLSoapServiceEditorComponent(MessageSource msg, SAMLEditorGeneralTab generalTab,
			SAMLEditorClientsTab clientsTab, IdpEditorUsersTab usersTab,
			AuthenticationTab authTab, EndpointTypeDescription type, PKIManagement pkiMan,
			URIAccessService uriAccessService,
			VaadinLogoImageLoader imageAccessService,
			FileStorageService fileStorageService,
			ServiceDefinition toEdit, List<Group> allGroups)
	{
		super(msg);
		this.fileStorageService = fileStorageService;
		this.pkiMan = pkiMan;
		boolean editMode = toEdit != null;

		samlServiceBinder = new Binder<>(DefaultServiceDefinition.class);
		samlConfigBinder = new Binder<>(SAMLServiceConfiguration.class);

		generalTab.initUI(samlServiceBinder, samlConfigBinder, editMode);
		registerTab(generalTab);
		clientsTab.initUI(samlConfigBinder);
		registerTab(clientsTab);
		usersTab.initUI(samlConfigBinder);
		registerTab(usersTab);
		authTab.initUI(samlServiceBinder);
		registerTab(authTab);

		DefaultServiceDefinition serviceBean = new DefaultServiceDefinition(type.getName());
		SAMLServiceConfiguration samlConfig = new SAMLServiceConfiguration(msg, allGroups);

		if (editMode)
		{
			serviceBean = (DefaultServiceDefinition) toEdit;
			if (serviceBean.getConfiguration() != null)
			{
				samlConfig.fromProperties(serviceBean.getConfiguration(), msg, uriAccessService, 
						imageAccessService, allGroups);
			}
		}

		samlServiceBinder.setBean(serviceBean);
		samlConfigBinder.setBean(samlConfig);

		if (editMode)
		{
			samlServiceBinder.validate();
			samlConfigBinder.validate();
		}

		Runnable refreshClients = () -> usersTab.setAvailableClients(clientsTab.getActiveClients().stream()
				.collect(Collectors.toMap(SAMLIndividualTrustedSPConfiguration::getName, this::getCaption)));
		clientsTab.addClientsValueChangeListener(e -> refreshClients.run());
		refreshClients.run();
	}

	private String getCaption(SAMLIndividualTrustedSPConfiguration spConfiguration)
	{
		if(spConfiguration.getDisplayedName() == null || spConfiguration.getDisplayedName().isEmpty())
			return spConfiguration.getName();
		String value = spConfiguration.getDisplayedName().getValue(msg);
		return value.isEmpty() ? spConfiguration.getName() : value;
	}

	public ServiceDefinition getServiceDefiniton() throws FormValidationException
	{
		boolean hasErrors = samlServiceBinder.validate().hasErrors();
		hasErrors |= samlConfigBinder.validate().hasErrors();

		if (hasErrors)
		{
			setErrorInTabs();
			throw new FormValidationException();
		}

		DefaultServiceDefinition service = samlServiceBinder.getBean();

		try
		{
			service.setConfiguration(samlConfigBinder.getBean().toProperties(pkiMan, msg,
					fileStorageService, service.getName()));
		} catch (Exception e)
		{
			throw new FormValidationException("Invalid configuration of the SAML idp service", e);
		}

		return service;
	}

}
