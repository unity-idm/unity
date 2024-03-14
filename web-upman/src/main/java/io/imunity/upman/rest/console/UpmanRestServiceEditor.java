/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest.console;

import java.util.List;
import java.util.Set;

import io.imunity.upman.rest.RESTUpmanEndpoint;
import io.imunity.upman.rest.console.UpmanRestServiceConfiguration.UpmanRestServiceConfigurationProvider;
import io.imunity.vaadin.auth.services.DefaultServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceEditor;
import io.imunity.vaadin.auth.services.ServiceEditorComponent;
import io.imunity.vaadin.auth.services.tabs.AuthenticationTab;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.rest.jwt.endpoint.JWTManagementEndpoint;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;


class UpmanRestServiceEditor implements ServiceEditor
{
	private final MessageSource msg;
	private final UpmanRestServiceConfigurationProvider configProvider;
	private final List<String> allRealms;
	private final List<AuthenticationFlowDefinition> flows;
	private final List<AuthenticatorInfo> authenticators;
	private final List<String> usedPaths;
	private final List<String> usedNames;
	private final Set<String> serverContextPaths;
	private final List<Group> groups;
	private final List<String> attributes;
	private UpmanRestServiceEditorComponent editor;

	UpmanRestServiceEditor(MessageSource msg, UpmanRestServiceConfigurationProvider configProvider,
			List<String> allRealms, List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,
			List<String> usedPaths, List<String> usedNames, Set<String> serverContextPaths, List<Group> groups, List<String> attributes)
	{
		this.msg = msg;
		this.configProvider = configProvider;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.usedPaths = usedPaths;
		this.usedNames = usedNames;
		this.serverContextPaths = serverContextPaths;
		this.groups = groups;
		this.attributes = attributes;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{

		UpmanRestServiceEditorGeneralTab upmanRestServiceEditorGeneralTab = new UpmanRestServiceEditorGeneralTab(
				msg, RESTUpmanEndpoint.TYPE, usedPaths, usedNames, serverContextPaths, groups, attributes);

		AuthenticationTab authenticationTab = new AuthenticationTab(msg, flows, authenticators, allRealms,
				JWTManagementEndpoint.TYPE.getSupportedBinding());

		editor = new UpmanRestServiceEditorComponent(msg, configProvider, upmanRestServiceEditorGeneralTab, authenticationTab,
				(DefaultServiceDefinition) endpoint);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}

	
}
