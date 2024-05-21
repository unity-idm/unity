/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceEditorBase;
import io.imunity.vaadin.auth.services.layout.ServiceWebConfiguration;
import io.imunity.vaadin.auth.services.idp.IdpEditorUsersTab;
import io.imunity.vaadin.auth.services.idp.PolicyAgreementsTab;
import io.imunity.vaadin.auth.services.tabs.WebServiceAuthenticationTab;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.util.List;
import java.util.stream.Collectors;

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

	SAMLServiceEditorComponent(MessageSource msg, SAMLEditorGeneralTab generalTab,
			SAMLEditorClientsTab clientsTab, IdpEditorUsersTab usersTab,
			WebServiceAuthenticationTab webAuthTab, PolicyAgreementsTab policyAgreementTab, EndpointTypeDescription type, PKIManagement pkiMan,
			URIAccessService uriAccessService,
			VaadinLogoImageLoader imageAccessService,
			FileStorageService fileStorageService,
			ServiceDefinition toEdit, List<Group> allGroups)
	{
		super(msg);
		this.fileStorageService = fileStorageService;
		this.pkiMan = pkiMan;

		boolean editMode = toEdit != null;

		setWidthFull();
		samlServiceBinder = new Binder<>(DefaultServiceDefinition.class);
		samlConfigBinder = new Binder<>(SAMLServiceConfiguration.class);
		webConfigBinder = new Binder<>(ServiceWebConfiguration.class);
		
		samlConfigBinder.forField(policyAgreementTab)
				.asRequired()
				.bind(SAMLServiceConfiguration::getPolicyAgreementConfig, SAMLServiceConfiguration::setPolicyAgreementConfig);
		
		generalTab.initUI(samlServiceBinder, samlConfigBinder, editMode);
		registerTab(generalTab);
		clientsTab.initUI(samlConfigBinder);
		registerTab(clientsTab);
		usersTab.initUI(samlConfigBinder);
		registerTab(usersTab);
		webAuthTab.initUI(samlServiceBinder, webConfigBinder);
		registerTab(webAuthTab);
		registerTab(policyAgreementTab);
		
		
		DefaultServiceDefinition serviceBean = new DefaultServiceDefinition(type.getName());
		ServiceWebConfiguration webConfig = new ServiceWebConfiguration();
		SAMLServiceConfiguration samlConfig = new SAMLServiceConfiguration(msg, allGroups);

		if (editMode)
		{
			serviceBean = (DefaultServiceDefinition) toEdit;
			if (serviceBean.getConfiguration() != null)
			{
				webConfig.fromProperties(serviceBean.getConfiguration(), msg, imageAccessService);
				samlConfig.fromProperties(serviceBean.getConfiguration(), msg, uriAccessService, 
						imageAccessService, allGroups);
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
