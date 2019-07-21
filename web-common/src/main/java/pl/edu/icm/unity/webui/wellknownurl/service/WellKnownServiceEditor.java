/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.wellknownurl.service;

import java.util.List;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.webui.authn.services.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceEditor;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.wellknownurl.WellKnownURLEndpointFactory;

/**
 * 
 * @author P.Piernik
 *
 */
public class WellKnownServiceEditor implements ServiceEditor
{
	private UnityMessageSource msg;
	private List<String> allRealms;
	private List<AuthenticationFlowDefinition> flows;
	private List<AuthenticatorInfo> authenticators;
	private WellKnownServiceEditorComponent editor;

	public WellKnownServiceEditor(UnityMessageSource msg, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		editor = new WellKnownServiceEditorComponent(msg, endpoint, allRealms, flows, authenticators);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServicetDefiniton();
	}

	private class WellKnownServiceEditorComponent extends ServiceEditorBase
	{

		public WellKnownServiceEditorComponent(UnityMessageSource msg, ServiceDefinition toEdit,
				List<String> allRealms, List<AuthenticationFlowDefinition> flows,
				List<AuthenticatorInfo> authenticators)
		{
			super(msg, WellKnownURLEndpointFactory.TYPE, toEdit, allRealms, flows, authenticators);
		}

		@Override
		protected String getConfiguration(String serviceName) throws FormValidationException
		{
			return "";
		}

		@Override
		protected void validateConfiguration() throws FormValidationException
		{
		}

	}
}
