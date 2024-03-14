/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.secured_shared_endpoint.console;

import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.auth.services.*;
import io.imunity.vaadin.auth.services.tabs.AuthenticationTab;
import io.imunity.vaadin.auth.services.tabs.GeneralTab;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import io.imunity.vaadin.secured_shared_endpoint.SecuredSharedEndpointFactory;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;

import java.util.List;
import java.util.Set;

class WellKnownServiceEditor implements ServiceEditor
{
	private final MessageSource msg;
	private final List<String> allRealms;
	private final List<AuthenticationFlowDefinition> flows;
	private final List<AuthenticatorInfo> authenticators;
	private final List<String> usedEndpointsPaths;
	private final Set<String> serverContextPaths;
	private WellKnownServiceEditorComponent editor;

	WellKnownServiceEditor(MessageSource msg, List<String> allRealms,
	                       List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators, List<String> usedPaths,
	                       Set<String> serverContextPaths)
	{
		this.msg = msg;
		this.allRealms = List.copyOf(allRealms);
		this.authenticators = List.copyOf(authenticators);
		this.flows = List.copyOf(flows);
		this.usedEndpointsPaths = List.copyOf(usedPaths);
		this.serverContextPaths = serverContextPaths;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		
		GeneralTab generalTab = new GeneralTab(msg, SecuredSharedEndpointFactory.TYPE, usedEndpointsPaths, serverContextPaths);
		
		AuthenticationTab authenticationTab = new AuthenticationTab(msg, flows, authenticators, allRealms,
				SecuredSharedEndpointFactory.TYPE.getSupportedBinding());
		
		editor = new WellKnownServiceEditorComponent(msg, generalTab, authenticationTab, (DefaultServiceDefinition) endpoint);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}

	private static class WellKnownServiceEditorComponent extends ServiceEditorBase
	{

		private final Binder<DefaultServiceDefinition> serviceBinder;

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
