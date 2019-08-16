/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.web.service;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.webui.authn.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceEditor;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * JWT service service editor
 * 
 * @author P.Piernik
 *
 */
public class JWTServiceEditor implements ServiceEditor
{
	private UnityMessageSource msg;
	private List<String> allRealms;
	private List<AuthenticationFlowDefinition> flows;
	private List<AuthenticatorInfo> authenticators;
	private Set<String> credentials;
	private JWTServiceEditorComponent editor;
	private List<String> usedPaths;

	public JWTServiceEditor(UnityMessageSource msg, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,
			Set<String> credentials, List<String> usedPaths)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.credentials = credentials;
		this.usedPaths = usedPaths;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		editor = new JWTServiceEditorComponent(msg, (DefaultServiceDefinition) endpoint, allRealms, flows,
				authenticators, credentials, usedPaths);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
