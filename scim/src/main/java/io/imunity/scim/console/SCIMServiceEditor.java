/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim.console;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.scim.SCIMEndpoint;
import io.imunity.scim.console.SCIMServiceEditorSchemaTab.SCIMServiceEditorSchemaTabFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.rest.jwt.endpoint.JWTManagementEndpoint;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent;
import pl.edu.icm.unity.webui.console.services.tabs.AuthenticationTab;

class SCIMServiceEditor implements ServiceEditor
{
	private final MessageSource msg;
	private final List<String> allRealms;
	private final List<Group> allGroups;
	private final List<AuthenticationFlowDefinition> flows;
	private final List<AuthenticatorInfo> authenticators;
	private final List<String> usedPaths;
	private final Set<String> serverContextPaths;
	private final SubViewSwitcher subViewSwitcher;
	private SCIMServiceEditorComponent editor;
	private final SCIMServiceEditorSchemaTabFactory editorSchemaTabFactory;

	SCIMServiceEditor(MessageSource msg, SubViewSwitcher subViewSwitcher, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators, List<String> usedPaths,
			Set<String> serverContextPaths, List<Group> allGroups,
			SCIMServiceEditorSchemaTabFactory editorSchemaTabFactory)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.usedPaths = usedPaths;
		this.serverContextPaths = serverContextPaths;
		this.allGroups = allGroups;
		this.subViewSwitcher = subViewSwitcher;
		this.editorSchemaTabFactory = editorSchemaTabFactory;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{

		SCIMServiceEditorGeneralTab restAdminServiceEditorGeneralTab = new SCIMServiceEditorGeneralTab(msg,
				SCIMEndpoint.TYPE, usedPaths, serverContextPaths, allGroups);

		AuthenticationTab authenticationTab = new AuthenticationTab(msg, flows, authenticators, allRealms,
				JWTManagementEndpoint.TYPE.getSupportedBinding());

		SCIMServiceEditorSchemaTab schemaTab = editorSchemaTabFactory.getSCIMServiceEditorSchemaTab(subViewSwitcher);

		editor = new SCIMServiceEditorComponent(msg, restAdminServiceEditorGeneralTab, authenticationTab, schemaTab,
				(DefaultServiceDefinition) endpoint);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}

	@Component
	static class SCIMServiceEditorFactory
	{
		private final MessageSource msg;
		private final EndpointManagement endpointMan;
		private final RealmsManagement realmsMan;
		private final AuthenticationFlowManagement flowsMan;
		private final AuthenticatorManagement authMan;
		private final NetworkServer networkServer;
		private final BulkGroupQueryService bulkService;
		private final SCIMServiceEditorSchemaTabFactory editorSchemaTabFactory;

		@Autowired
		SCIMServiceEditorFactory(MessageSource msg, EndpointManagement endpointMan, RealmsManagement realmsMan,
				AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan, NetworkServer networkServer,
				BulkGroupQueryService bulkService,
				SCIMServiceEditorSchemaTabFactory editorSchemaTabFactory)
		{
			this.msg = msg;
			this.endpointMan = endpointMan;
			this.realmsMan = realmsMan;
			this.flowsMan = flowsMan;
			this.authMan = authMan;
			this.networkServer = networkServer;
			this.bulkService = bulkService;
			this.editorSchemaTabFactory = editorSchemaTabFactory;
		}

		public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
		{
			return new SCIMServiceEditor(msg, subViewSwitcher,
					realmsMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toList()),
					flowsMan.getAuthenticationFlows().stream().collect(Collectors.toList()),
					authMan.getAuthenticators(null).stream().collect(Collectors.toList()),
					endpointMan.getEndpoints().stream().map(e -> e.getContextAddress()).collect(Collectors.toList()),
					networkServer.getUsedContextPaths(),
					bulkService.getGroupAndSubgroups(bulkService.getBulkStructuralData("/")).values().stream()
							.map(g -> g.getGroup()).collect(Collectors.toList()),
					editorSchemaTabFactory);
		}
	}

}
