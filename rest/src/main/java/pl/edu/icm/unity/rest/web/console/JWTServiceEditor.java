/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.web.console;

import java.util.List;
import java.util.Set;

import io.imunity.vaadin.endpoint.common.api.services.DefaultServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditor;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent;
import io.imunity.vaadin.endpoint.common.api.services.tabs.AuthenticationTab;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.rest.jwt.endpoint.JWTManagementEndpoint;
import pl.edu.icm.unity.webui.common.FormValidationException;


/**
 * JWT service service editor
 * 
 * @author P.Piernik
 *
 */
class JWTServiceEditor implements ServiceEditor
{
	private final MessageSource msg;
	private final List<String> allRealms;
	private final List<AuthenticationFlowDefinition> flows;
	private final List<AuthenticatorInfo> authenticators;
	private final Set<String> credentials;
	private final List<String> usedEndpointsPaths;
	private final List<String> usedNames;
	private final Set<String> serverContextPaths;
	private JWTServiceEditorComponent editor;

	JWTServiceEditor(MessageSource msg, List<String> allRealms, List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, Set<String> credentials, List<String> usedPaths,
			List<String> usedNames, Set<String> serverContextPaths)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.credentials = credentials;
		this.usedEndpointsPaths = usedPaths;
		this.usedNames = usedNames;
		this.serverContextPaths = serverContextPaths;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{

		JWTServiceEditorGeneralTab generalTab = new JWTServiceEditorGeneralTab(msg, JWTManagementEndpoint.TYPE,
				usedEndpointsPaths, usedNames, serverContextPaths, credentials);

		AuthenticationTab authTab = new AuthenticationTab(msg, flows, authenticators, allRealms,
				JWTManagementEndpoint.TYPE.getSupportedBinding());

		editor = new JWTServiceEditorComponent(msg, generalTab, authTab, (DefaultServiceDefinition) endpoint);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
