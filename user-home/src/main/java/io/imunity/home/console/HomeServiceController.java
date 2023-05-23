/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.console;

import io.imunity.home.ProjectManagementHelper;
import io.imunity.home.UserHomeEndpointFactory;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.*;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.DefaultServicesControllerBase;
import pl.edu.icm.unity.webui.console.services.ServiceController;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;

import java.util.stream.Collectors;

@Component
class HomeServiceController extends DefaultServicesControllerBase implements ServiceController
{
	private RealmsManagement realmsMan;
	private AuthenticationFlowManagement flowsMan;
	private AuthenticatorManagement authMan;
	private AttributeTypeManagement atMan;
	private BulkGroupQueryService bulkService;
	private ProjectManagementHelper projectManHelper;
	private EnquiryManagement enquiryMan;
	private RegistrationsManagement registrationMan;
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private UnityServerConfiguration serverConfig;
	private AuthenticatorSupportService authenticatorSupportService;
	private NetworkServer server;
	private ImageAccessService imageAccessService;

	HomeServiceController(MessageSource msg, EndpointManagement endpointMan, RealmsManagement realmsMan,
						  AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan,
						  AttributeTypeManagement atMan, BulkGroupQueryService bulkService,
						  ProjectManagementHelper projectManagementHelper, EnquiryManagement enquiryMan,
						  RegistrationsManagement registrationMan, URIAccessService uriAccessService,
						  FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
						  AuthenticatorSupportService authenticatorSupportService, NetworkServer server,
						  ImageAccessService imageAccessService, EndpointFileConfigurationManagement serviceFileConfigController)
			
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
		this.uriAccessService = uriAccessService;
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

		return new HomeServiceEditor(msg, uriAccessService, imageAccessService, fileStorageService, serverConfig,
				realmsMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				flowsMan.getAuthenticationFlows().stream().collect(Collectors.toList()),
				authMan.getAuthenticators(null).stream().collect(Collectors.toList()),
				atMan.getAttributeTypes().stream().map(a -> a.getName()).collect(Collectors.toList()),
				atMan.getAttributeTypes().stream().filter(a -> a.getValueSyntax().equals("image")).map(a -> a.getName()).collect(Collectors.toList()),
				bulkService.getGroupAndSubgroups(bulkService.getBulkStructuralData("/")).values()
						.stream().map(g -> g.getGroup()).collect(Collectors.toList()),
				projectManHelper.getAllProjectManEndpoints().stream().map(e -> e.getName())
						.collect(Collectors.toList()),
				enquiryMan.getEnquires().stream().map(e -> e.getName()).collect(Collectors.toList()),
				registrationMan.getForms().stream().filter(r -> r.isPubliclyAvailable())
						.map(r -> r.getName()).collect(Collectors.toList()),
				endpointMan.getEndpoints().stream().map(e -> e.getContextAddress())
						.collect(Collectors.toList()), server.getUsedContextPaths(),
				authenticatorSupportService);
	}
}
