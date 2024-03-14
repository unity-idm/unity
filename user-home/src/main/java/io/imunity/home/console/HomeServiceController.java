/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.console;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.imunity.home.ProjectManagementHelper;
import io.imunity.home.UserHomeEndpointFactory;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.auth.services.DefaultServicesControllerBase;
import io.imunity.vaadin.auth.services.ServiceController;
import io.imunity.vaadin.auth.services.ServiceEditor;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;

@Component
class HomeServiceController extends DefaultServicesControllerBase implements ServiceController
{
	private final RealmsManagement realmsMan;
	private final AuthenticationFlowManagement flowsMan;
	private final AuthenticatorManagement authMan;
	private final AttributeTypeManagement atMan;
	private final BulkGroupQueryService bulkService;
	private final ProjectManagementHelper projectManHelper;
	private final EnquiryManagement enquiryMan;
	private final RegistrationsManagement registrationMan;
	private final FileStorageService fileStorageService;
	private final UnityServerConfiguration serverConfig;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final NetworkServer server;
	private final VaadinLogoImageLoader imageAccessService;

	HomeServiceController(MessageSource msg, EndpointManagement endpointMan, RealmsManagement realmsMan,
						  AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan,
						  AttributeTypeManagement atMan, BulkGroupQueryService bulkService,
						  ProjectManagementHelper projectManagementHelper, EnquiryManagement enquiryMan,
						  RegistrationsManagement registrationMan,
						  FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
						  AuthenticatorSupportService authenticatorSupportService, NetworkServer server,
						  VaadinLogoImageLoader imageAccessService, EndpointFileConfigurationManagement serviceFileConfigController)
			
	{
		super(msg, endpointMan, serviceFileConfigController);
		this.realmsMan = realmsMan;
		this.flowsMan = flowsMan;
		this.authMan = authMan;
		this.atMan = atMan;
		this.bulkService = bulkService;
		this.projectManHelper = projectManagementHelper;
		this.enquiryMan = enquiryMan;
		this.registrationMan = registrationMan;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.server = server;
		this.imageAccessService = imageAccessService;
	}

	@Override
	public String getSupportedEndpointType()
	{
		return UserHomeEndpointFactory.NAME;
	}

	@Override
	public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
	{

		return new HomeServiceEditor(msg, imageAccessService, fileStorageService, serverConfig,
				realmsMan.getRealms().stream().map(DescribedObjectROImpl::getName).collect(Collectors.toList()),
				new ArrayList<>(flowsMan.getAuthenticationFlows()),
				new ArrayList<>(authMan.getAuthenticators(null)),
				atMan.getAttributeTypes().stream().map(AttributeType::getName).collect(Collectors.toList()),
				atMan.getAttributeTypes().stream().filter(a -> a.getValueSyntax().equals( ImageAttributeSyntax.ID))
						.map(AttributeType::getName)
						.sorted()
						.collect(Collectors.toList()),
				bulkService.getGroupAndSubgroups(bulkService.getBulkStructuralData("/")).values()
						.stream().map(GroupContents::getGroup).collect(Collectors.toList()),
				projectManHelper.getAllProjectManEndpoints().stream().map(ResolvedEndpoint::getName)
						.collect(Collectors.toList()),
				enquiryMan.getEnquires().stream().map(DescribedObjectROImpl::getName).collect(Collectors.toList()),
				registrationMan.getForms().stream().filter(RegistrationForm::isPubliclyAvailable)
						.map(DescribedObjectROImpl::getName).collect(Collectors.toList()),
				endpointMan.getEndpoints().stream().map(Endpoint::getContextAddress)
						.collect(Collectors.toList()), server.getUsedContextPaths(),
				authenticatorSupportService);
	}
}
