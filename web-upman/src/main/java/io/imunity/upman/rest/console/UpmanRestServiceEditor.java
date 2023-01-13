/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest.console;

import io.imunity.upman.rest.RESTUpmanEndpoint;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.rest.jwt.endpoint.JWTManagementEndpoint;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent;
import pl.edu.icm.unity.webui.console.services.tabs.AuthenticationTab;

import java.util.List;
import java.util.Set;

class UpmanRestServiceEditor implements ServiceEditor
{
	private final MessageSource msg;
	private final List<String> allRealms;
	private final List<AuthenticationFlowDefinition> flows;
	private final List<AuthenticatorInfo> authenticators;
	private final List<String> usedPaths;
	private final Set<String> serverContextPaths;
	private final List<Group> groups;
	private UpmanRestServiceEditorComponent editor;

	UpmanRestServiceEditor(MessageSource msg, List<String> allRealms, List<AuthenticationFlowDefinition> flows,
	                       List<AuthenticatorInfo> authenticators, List<String> usedPaths,
	                       Set<String> serverContextPaths, List<Group> groups)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.usedPaths = usedPaths;
		this.serverContextPaths = serverContextPaths;
		this.groups = groups;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{

		UpmanRestServiceEditorGeneralTab upmanRestServiceEditorGeneralTab = new UpmanRestServiceEditorGeneralTab(
				msg, RESTUpmanEndpoint.TYPE, usedPaths, serverContextPaths, groups);

		AuthenticationTab authenticationTab = new AuthenticationTab(msg, flows, authenticators, allRealms,
				JWTManagementEndpoint.TYPE.getSupportedBinding());

		editor = new UpmanRestServiceEditorComponent(msg, upmanRestServiceEditorGeneralTab, authenticationTab,
				(DefaultServiceDefinition) endpoint);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
