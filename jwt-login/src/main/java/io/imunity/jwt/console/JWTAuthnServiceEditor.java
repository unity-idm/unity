/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt.console;

import java.util.List;
import java.util.Set;

import io.imunity.jwt.JWTAuthzWebEndpointFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.rest.jwt.endpoint.JWTManagementEndpoint;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent;
import pl.edu.icm.unity.webui.console.services.tabs.AuthenticationTab;

class JWTAuthnServiceEditor implements ServiceEditor
{
	private MessageSource msg;
	private List<String> allRealms;
	private List<AuthenticationFlowDefinition> flows;
	private List<AuthenticatorInfo> authenticators;
	private Set<String> credentials;
	private JWTAuthnServiceEditorComponent editor;
	private List<String> usedEndpointsPaths;
	private Set<String> serverContextPaths;

	JWTAuthnServiceEditor(MessageSource msg, List<String> allRealms, List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, Set<String> credentials, List<String> usedPaths, Set<String> serverContextPaths)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.credentials = credentials;
		this.usedEndpointsPaths = usedPaths;
		this.serverContextPaths = serverContextPaths;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{

		JWTAuthnServiceEditorGeneralTab generalTab = new JWTAuthnServiceEditorGeneralTab(msg,
				JWTAuthzWebEndpointFactory.TYPE, usedEndpointsPaths, serverContextPaths, credentials);

		AuthenticationTab authTab = new AuthenticationTab(msg, flows, authenticators, allRealms,
				JWTManagementEndpoint.TYPE.getSupportedBinding());

		editor = new JWTAuthnServiceEditorComponent(msg, generalTab, authTab,
				endpoint == null ? null : ((JWTAuthnServiceDefinition) endpoint).webAuthzService);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
