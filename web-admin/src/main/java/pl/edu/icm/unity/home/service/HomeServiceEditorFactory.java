/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.home.service;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.home.UserHomeEndpointFactory;
import pl.edu.icm.unity.webadmin.utils.ProjectManagementHelper;
import pl.edu.icm.unity.webui.authn.services.ServiceEditor;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorFactory;
import pl.edu.icm.unity.webui.providers.HomeUITabProvider;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
public class HomeServiceEditorFactory implements ServiceEditorFactory
{
	private UnityMessageSource msg;
	private RealmsManagement realmsMan;
	private AuthenticationFlowManagement flowsMan;
	private AuthenticatorManagement authMan;
	private HomeUITabProvider tabProvider;
	private AttributeTypeManagement atMan;
	private BulkGroupQueryService bulkService;
	private ProjectManagementHelper projectManHelper;
	private EnquiryManagement enquiryMan;
	private RegistrationsManagement registrationMan;
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private UnityServerConfiguration serverConfig;

	private AuthenticatorSupportService authenticatorSupportService;

	@Autowired
	public HomeServiceEditorFactory(UnityMessageSource msg, RealmsManagement realmsMan,
			AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan,
			HomeUITabProvider tabProvider, AttributeTypeManagement atMan, BulkGroupQueryService bulkService,
			ProjectManagementHelper projectManagementHelper, EnquiryManagement enquiryMan,
			RegistrationsManagement registrationMan, URIAccessService uriAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			AuthenticatorSupportService authenticatorSupportService)
	{
		this.msg = msg;
		this.realmsMan = realmsMan;
		this.flowsMan = flowsMan;
		this.authMan = authMan;
		this.tabProvider = tabProvider;
		this.atMan = atMan;
		this.bulkService = bulkService;
		this.projectManHelper = projectManagementHelper;
		this.enquiryMan = enquiryMan;
		this.registrationMan = registrationMan;
		this.uriAccessService = uriAccessService;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
	}

	@Override
	public String getSupportedEndpointType()
	{
		return UserHomeEndpointFactory.NAME;
	}

	@Override
	public ServiceEditor createInstance() throws EngineException
	{

		return new HomeServiceEditor(msg, uriAccessService, fileStorageService, serverConfig,
				realmsMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				flowsMan.getAuthenticationFlows().stream().collect(Collectors.toList()),
				authMan.getAuthenticators(null).stream().collect(Collectors.toList()),
				tabProvider.getId(),
				atMan.getAttributeTypes().stream().map(a -> a.getName()).collect(Collectors.toList()),
				bulkService.getGroupAndSubgroups(bulkService.getBulkStructuralData("/")).values()
						.stream().map(g -> g.getGroup()).collect(Collectors.toList()),
				projectManHelper.getAllProjectManEndpoints().stream().map(e -> e.getName())
						.collect(Collectors.toList()),
				enquiryMan.getEnquires().stream().map(e -> e.getName()).collect(Collectors.toList()),
				registrationMan.getForms().stream().filter(r -> r.isPubliclyAvailable()).map(r -> r.getName()).collect(Collectors.toList()),
				authenticatorSupportService);
	}
}