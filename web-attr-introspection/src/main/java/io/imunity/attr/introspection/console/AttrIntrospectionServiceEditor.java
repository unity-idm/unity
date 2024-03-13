/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.attr.introspection.AttrIntrospectionEndpointFactory;
import io.imunity.attr.introspection.config.AttrIntrospectionAttributePoliciesConfiguration;
import io.imunity.vaadin.endpoint.common.api.services.DefaultServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditor;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorBase;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent;
import io.imunity.vaadin.endpoint.common.api.services.tabs.GeneralTab;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.IdPInfo;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.common.FormValidationException;

public class AttrIntrospectionServiceEditor implements ServiceEditor
{
	private final MessageSource msg;
	private final List<String> usedEndpointsPaths;
	private final List<String> usedNames;
	private final Set<String> serverContextPaths;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final Supplier<Set<String>> authnOptionSupplier;
	private final Supplier<List<IdPInfo>> providersSupplier;
	private final FileStorageService fileStorageService;
	private final VaadinLogoImageLoader imageAccessService;
	private final UnityServerConfiguration serverConfig;
	private AttrIntrospectionEditorComponent editor;

	public AttrIntrospectionServiceEditor(MessageSource msg, List<String> usedPaths, List<String> usedNames, Set<String> serverContextPaths,
			AuthenticatorSupportService authenticatorSupportService, Supplier<Set<String>> authnOptionSupplier,
			Supplier<List<IdPInfo>> providersSupplier, FileStorageService fileStorageService,
			VaadinLogoImageLoader imageAccessService, UnityServerConfiguration serverConfig)
	{
		this.msg = msg;
		this.usedEndpointsPaths = usedPaths;
		this.usedNames = usedNames;
		this.serverContextPaths = serverContextPaths;
		this.authenticatorSupportService = authenticatorSupportService;
		this.authnOptionSupplier = authnOptionSupplier;
		this.providersSupplier = providersSupplier;
		this.fileStorageService = fileStorageService;
		this.imageAccessService = imageAccessService;
		this.serverConfig = serverConfig;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		GeneralTab generalTab = new GeneralTab(msg, AttrIntrospectionEndpointFactory.TYPE, usedEndpointsPaths, usedNames,
				serverContextPaths);
		AuthenticationOptionsTab authenticationOptionsTab = new AuthenticationOptionsTab(msg, serverConfig,
				authenticatorSupportService, authnOptionSupplier);
		AttributePoliciesTab attributePoliciesTab = new AttributePoliciesTab(msg, providersSupplier);

		editor = new AttrIntrospectionEditorComponent(msg, generalTab, authenticationOptionsTab, attributePoliciesTab,
				(DefaultServiceDefinition) endpoint);
		return editor;
	}

	@Override
	public ServiceDefinition getEndpointDefiniton() throws FormValidationException
	{
		return editor.getServiceDefiniton();
	}

	private class AttrIntrospectionEditorComponent extends ServiceEditorBase
	{

		private Binder<DefaultServiceDefinition> serviceBinder;
		private Binder<AttrIntrospectionAuthnScreenConfiguration> authnScreenConfigBinder;
		private Binder<AttrIntrospectionAttributePoliciesConfiguration> attrPoliciesBinder;

		AttrIntrospectionEditorComponent(MessageSource msg, GeneralTab generalTab,
				AuthenticationOptionsTab authenticationOptionsTab, AttributePoliciesTab attributePoliciesTab,
				DefaultServiceDefinition toEdit)
		{
			super(msg);
			boolean editMode = toEdit != null;
			serviceBinder = new Binder<>(DefaultServiceDefinition.class);
			authnScreenConfigBinder = new Binder<>(AttrIntrospectionAuthnScreenConfiguration.class);
			attrPoliciesBinder = new Binder<>(AttrIntrospectionAttributePoliciesConfiguration.class);

			generalTab.initUI(serviceBinder, editMode);
			authenticationOptionsTab.initUI(authnScreenConfigBinder);
			attributePoliciesTab.initUI(attrPoliciesBinder);

			registerTab(generalTab);
			registerTab(authenticationOptionsTab);
			registerTab(attributePoliciesTab);

			serviceBinder.setBean(
					editMode ? toEdit : new DefaultServiceDefinition(AttrIntrospectionEndpointFactory.TYPE.getName()));

			AttrIntrospectionAuthnScreenConfiguration screenConfig = new AttrIntrospectionAuthnScreenConfiguration();
			AttrIntrospectionAttributePoliciesConfiguration policiesConfig = new AttrIntrospectionAttributePoliciesConfiguration();

			if (editMode && toEdit.getConfiguration() != null)
			{
				screenConfig.fromProperties(toEdit.getConfiguration(), msg, imageAccessService);
				policiesConfig.fromProperties(toEdit.getConfiguration(), msg);
			}
			authnScreenConfigBinder.setBean(screenConfig);
			attrPoliciesBinder.setBean(policiesConfig);
		}

		public ServiceDefinition getServiceDefiniton() throws FormValidationException
		{
			boolean hasErrors = serviceBinder.validate()
					.hasErrors();
			hasErrors |= authnScreenConfigBinder.validate()
					.hasErrors();
			hasErrors |= attrPoliciesBinder.validate()
					.hasErrors();
			if (hasErrors)
			{
				setErrorInTabs();
				throw new FormValidationException();
			}

			DefaultServiceDefinition service = serviceBinder.getBean();
			VaadinEndpointProperties prop = new VaadinEndpointProperties(authnScreenConfigBinder.getBean()
					.toProperties(msg, fileStorageService, service.getName()));
			try
			{
				service.setConfiguration(prop.getAsString() + "\n" + attrPoliciesBinder.getBean()
						.toProperties(msg));
			} catch (JsonProcessingException e)
			{
				throw new FormValidationException();
			}
			return service;
		}
	}
}
