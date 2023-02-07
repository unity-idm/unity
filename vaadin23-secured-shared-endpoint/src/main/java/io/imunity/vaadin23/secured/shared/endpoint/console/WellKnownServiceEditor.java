/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.secured.shared.endpoint.console;

import com.vaadin.data.Binder;
import io.imunity.vaadin23.secured.shared.endpoint.SecuredSharedEndpointFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.console.services.*;
import pl.edu.icm.unity.webui.console.services.tabs.AuthenticationTab;
import pl.edu.icm.unity.webui.console.services.tabs.GeneralTab;

import java.util.List;
import java.util.Set;

/**
 * 
 * @author P.Piernik
 *
 */
class WellKnownServiceEditor implements ServiceEditor
{
	private MessageSource msg;
	private List<String> allRealms;
	private List<AuthenticationFlowDefinition> flows;
	private List<AuthenticatorInfo> authenticators;
	private WellKnownServiceEditorComponent2 editor;
	private List<String> usedEndpointsPaths;
	private Set<String> serverContextPaths;

	WellKnownServiceEditor(MessageSource msg, List<String> allRealms,
	                       List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators, List<String> usedPaths,
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
		
		GeneralTab generalTab = new GeneralTab(msg, SecuredSharedEndpointFactory.TYPE, usedEndpointsPaths, serverContextPaths);
		
		AuthenticationTab authenticationTab = new AuthenticationTab(msg, flows, authenticators, allRealms,
				SecuredSharedEndpointFactory.TYPE.getSupportedBinding());
		
		editor = new WellKnownServiceEditorComponent2(msg, generalTab, authenticationTab, (DefaultServiceDefinition) endpoint);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}

	private class WellKnownServiceEditorComponent2 extends ServiceEditorBase
	{

		private Binder<DefaultServiceDefinition> serviceBinder;

		public WellKnownServiceEditorComponent2(MessageSource msg, GeneralTab generalTab, AuthenticationTab authTab,
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
					: new DefaultServiceDefinition(SecuredSharedEndpointFactory.TYPE.getName()));

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
