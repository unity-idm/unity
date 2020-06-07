/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.wellknownurl.service;

import java.util.List;
import java.util.Set;

import com.vaadin.data.Binder;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent;
import pl.edu.icm.unity.webui.console.services.tabs.AuthenticationTab;
import pl.edu.icm.unity.webui.console.services.tabs.GeneralTab;
import pl.edu.icm.unity.webui.wellknownurl.WellKnownURLEndpointFactory;

/**
 * 
 * @author P.Piernik
 *
 */
public class WellKnownServiceEditor implements ServiceEditor
{
	private MessageSource msg;
	private List<String> allRealms;
	private List<AuthenticationFlowDefinition> flows;
	private List<AuthenticatorInfo> authenticators;
	private WellKnownServiceEditorComponent editor;
	private List<String> usedEndpointsPaths;
	private Set<String> serverContextPaths;

	public WellKnownServiceEditor(MessageSource msg, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,  List<String> usedPaths,
			Set<String> serverContextPaths)
	{
		this.msg = msg;
		this.allRealms = allRealms;
		this.authenticators = authenticators;
		this.flows = flows;
		this.usedEndpointsPaths = usedPaths;
		this.serverContextPaths = serverContextPaths;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		
		GeneralTab generalTab = new GeneralTab(msg, WellKnownURLEndpointFactory.TYPE, usedEndpointsPaths, serverContextPaths);
		
		AuthenticationTab authenticationTab = new AuthenticationTab(msg, flows, authenticators, allRealms,
				WellKnownURLEndpointFactory.TYPE.getSupportedBinding());
		
		editor = new WellKnownServiceEditorComponent(msg, generalTab, authenticationTab, (DefaultServiceDefinition) endpoint);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}

	private class WellKnownServiceEditorComponent extends ServiceEditorBase
	{

		private Binder<DefaultServiceDefinition> serviceBinder;

		public WellKnownServiceEditorComponent(MessageSource msg, GeneralTab generalTab, AuthenticationTab authTab,
				DefaultServiceDefinition toEdit)
		{
			super(msg);
			boolean editMode = toEdit != null;
			serviceBinder = new Binder<>(DefaultServiceDefinition.class);

			generalTab.initUI(serviceBinder, editMode);
			registerTab(generalTab);
			authTab.initUI(serviceBinder);
			registerTab(authTab);
			serviceBinder.setBean(editMode ? toEdit
					: new DefaultServiceDefinition(WellKnownURLEndpointFactory.TYPE.getName()));

		}

		public ServiceDefinition getServiceDefiniton() throws FormValidationException
		{
			boolean hasErrors = serviceBinder.validate().hasErrors();
			if (hasErrors)
			{
				setErrorInTabs();
				throw new FormValidationException();
			}

			DefaultServiceDefinition service = serviceBinder.getBean();
			service.setConfiguration("");
			return service;
		}

	}

}
