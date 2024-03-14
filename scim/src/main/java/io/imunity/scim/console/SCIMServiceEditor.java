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
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceEditor;
import io.imunity.vaadin.auth.services.ServiceEditorComponent;
import io.imunity.vaadin.auth.services.tabs.AuthenticationTab;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.rest.jwt.endpoint.JWTManagementEndpoint;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;


class SCIMServiceEditor implements ServiceEditor
{
	private final MessageSource msg;
	private final List<String> allRealms;
	private final List<Group> allGroups;
	private final List<AuthenticationFlowDefinition> flows;
	private final List<AuthenticatorInfo> authenticators;
	private final List<String> usedPaths;
	private final List<String> usedNames;
	private final Set<String> serverContextPaths;
	private final SubViewSwitcher subViewSwitcher;
	private SCIMServiceEditorComponent editor;
	private final SCIMServiceEditorSchemaTabFactory editorSchemaTabFactory;
	private final ConfigurationVaadinBeanMapper configurationVaadinBeanMapper;

	SCIMServiceEditor(MessageSource msg, SubViewSwitcher subViewSwitcher, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators, List<String> usedPaths, List<String> usedNames,
			Set<String> serverContextPaths, List<Group> allGroups,
			SCIMServiceEditorSchemaTabFactory editorSchemaTabFactory,
			ConfigurationVaadinBeanMapper configurationVaadinBeanMapper)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.usedPaths = usedPaths;
		this.usedNames = usedNames;
		this.serverContextPaths = serverContextPaths;
		this.allGroups = allGroups;
		this.subViewSwitcher = subViewSwitcher;
		this.editorSchemaTabFactory = editorSchemaTabFactory;
		this.configurationVaadinBeanMapper = configurationVaadinBeanMapper;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{

		SCIMServiceEditorGeneralTab restAdminServiceEditorGeneralTab = new SCIMServiceEditorGeneralTab(msg,
				SCIMEndpoint.TYPE, usedPaths, usedNames, serverContextPaths, allGroups);

		AuthenticationTab authenticationTab = new AuthenticationTab(msg, flows, authenticators, allRealms,
				JWTManagementEndpoint.TYPE.getSupportedBinding());

		SCIMServiceEditorSchemaTab schemaTab = editorSchemaTabFactory.getSCIMServiceEditorSchemaTab(subViewSwitcher);

		editor = new SCIMServiceEditorComponent(msg, configurationVaadinBeanMapper, restAdminServiceEditorGeneralTab, authenticationTab, schemaTab,
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
		private final ConfigurationVaadinBeanMapper configurationVaadinBeanMapper;

		@Autowired
		SCIMServiceEditorFactory(MessageSource msg, EndpointManagement endpointMan, RealmsManagement realmsMan,
				AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan, NetworkServer networkServer,
				BulkGroupQueryService bulkService,
				SCIMServiceEditorSchemaTabFactory editorSchemaTabFactory,
				ConfigurationVaadinBeanMapper configurationVaadinBeanMapper)
		{
			this.msg = msg;
			this.endpointMan = endpointMan;
			this.realmsMan = realmsMan;
			this.flowsMan = flowsMan;
			this.authMan = authMan;
			this.networkServer = networkServer;
			this.bulkService = bulkService;
			this.editorSchemaTabFactory = editorSchemaTabFactory;
			this.configurationVaadinBeanMapper = configurationVaadinBeanMapper;
		}

		public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
		{
			return new SCIMServiceEditor(msg, subViewSwitcher,
					realmsMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toList()),
					flowsMan.getAuthenticationFlows().stream().collect(Collectors.toList()),
					authMan.getAuthenticators(null).stream().collect(Collectors.toList()),
					endpointMan.getEndpoints().stream().map(e -> e.getContextAddress()).collect(Collectors.toList()),
					endpointMan.getEndpoints().stream().map(e -> e.getName()).collect(Collectors.toList()),
					networkServer.getUsedContextPaths(),
					bulkService.getGroupAndSubgroups(bulkService.getBulkStructuralData("/")).values().stream()
							.map(g -> g.getGroup()).collect(Collectors.toList()),
					editorSchemaTabFactory, configurationVaadinBeanMapper);
		}
	}

}
