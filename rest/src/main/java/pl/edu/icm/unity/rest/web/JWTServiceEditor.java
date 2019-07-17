/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.web;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.webui.authn.endpoints.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.endpoints.ServiceEditor;
import pl.edu.icm.unity.webui.authn.endpoints.ServiceEditorComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
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

	public JWTServiceEditor(UnityMessageSource msg, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators, Set<String> credentials)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.credentials = credentials;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		editor = new JWTServiceEditorComponent(msg, endpoint, allRealms, flows, authenticators, credentials);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServicetDefiniton();
	}
}
