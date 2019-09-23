/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.web.console;

import java.util.List;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent;

/**
 * 
 * Rest admin service editor
 * 
 * @author P.Piernik
 *
 */
class RestAdminServiceEditor implements ServiceEditor
{
	private UnityMessageSource msg;
	private List<String> allRealms;
	private List<AuthenticationFlowDefinition> flows;
	private List<AuthenticatorInfo> authenticators;
	private RestAdminServiceEditorComponent editor;
	private List<String> usedPaths;

	RestAdminServiceEditor(UnityMessageSource msg, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,
			List<String> usedPaths)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.usedPaths = usedPaths;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		editor = new RestAdminServiceEditorComponent(msg, (DefaultServiceDefinition) endpoint, allRealms, flows,
				authenticators, usedPaths);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}
}
