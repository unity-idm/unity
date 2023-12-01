/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest.console;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.imunity.upman.rest.RESTUpmanEndpoint;
import io.imunity.upman.rest.console.UpmanRestServiceConfiguration.UpmanRestServiceConfigurationProvider;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.DefaultServicesControllerBase;
import pl.edu.icm.unity.webui.console.services.ServiceController;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;

@Component
class UpmanRestServiceController extends DefaultServicesControllerBase implements ServiceController
{
	private final RealmsManagement realmsMan;
	private final AuthenticationFlowManagement flowsMan;
	private final AuthenticatorManagement authMan;
	private final NetworkServer networkServer;
	private final BulkGroupQueryService bulkService;
	private final AttributeSupport attributeSupport;
	private final UpmanRestServiceConfigurationProvider configProvider;


	UpmanRestServiceController(MessageSource msg, EndpointManagement endpointMan, RealmsManagement realmsMan,
	                           AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan, NetworkServer networkServer,
	                           EndpointFileConfigurationManagement serviceFileConfigController,  BulkGroupQueryService bulkService,
	                           AttributeSupport attributeSupport, UpmanRestServiceConfigurationProvider configProvider)
	{
		super(msg, endpointMan, serviceFileConfigController);
		this.realmsMan = realmsMan;
		this.flowsMan = flowsMan;
		this.authMan = authMan;
		this.networkServer = networkServer;
		this.bulkService = bulkService;
		this.attributeSupport = attributeSupport;
		this.configProvider = configProvider;
	}

	@Override
	public String getSupportedEndpointType()
	{
		return RESTUpmanEndpoint.NAME;
	}

	@Override
	public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
	{
		return new UpmanRestServiceEditor(msg, configProvider, realmsMan.getRealms().stream().map(DescribedObjectROImpl::getName)
				.collect(Collectors.toList()), new ArrayList<>(flowsMan.getAuthenticationFlows()),
				new ArrayList<>(authMan.getAuthenticators(null)), endpointMan.getEndpoints().stream().map(Endpoint::getContextAddress)
						.collect(Collectors.toList()),
				networkServer.getUsedContextPaths(),
				bulkService.getGroupAndSubgroups(bulkService.getBulkStructuralData("/")).values().stream().map(g -> g.getGroup())
						.collect(Collectors.toList()),
						attributeSupport.getAttributeTypesAsMap().values().stream().map(a -> a.getName()).collect(Collectors.toList())
				);
						
	}
}
