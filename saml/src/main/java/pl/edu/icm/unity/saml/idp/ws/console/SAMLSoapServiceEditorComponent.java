/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.ws.console;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;

import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.saml.idp.console.v8.SAMLEditorClientsTab;
import pl.edu.icm.unity.saml.idp.console.v8.SAMLEditorGeneralTab;
import pl.edu.icm.unity.saml.idp.console.v8.SAMLServiceConfiguration;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.console.services.idp.IdpEditorUsersTab;
import pl.edu.icm.unity.webui.console.services.tabs.AuthenticationTab;

/**
 * SAML SAOP Service editor ui component. It consists of 4 tabs: general,
 * clients, users and authentication
 * 
 * @author P.Piernik
 *
 */
class SAMLSoapServiceEditorComponent extends ServiceEditorBase
{
	private FileStorageService fileStorageService;
	private PKIManagement pkiMan;
	private Binder<SAMLServiceConfiguration> samlConfigBinder;
	private Binder<DefaultServiceDefinition> samlServiceBinder;

	SAMLSoapServiceEditorComponent(MessageSource msg, SAMLEditorGeneralTab generalTab,
			SAMLEditorClientsTab clientsTab, IdpEditorUsersTab usersTab,
			AuthenticationTab authTab, EndpointTypeDescription type, PKIManagement pkiMan,
			URIAccessService uriAccessService,
			ImageAccessService imageAccessService,
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
						imageAccessService, pkiMan, allGroups);
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
