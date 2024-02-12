/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console.v8;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.data.Binder;

import io.imunity.attr.introspection.AttrIntrospectionEndpointFactory;
import io.imunity.attr.introspection.config.AttrIntrospectionAttributePoliciesConfiguration;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.IdPInfo;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent;
import pl.edu.icm.unity.webui.console.services.tabs.GeneralTab;

public class AttrIntrospectionServiceEditor implements ServiceEditor
{
	private MessageSource msg;
	private AttrIntrospectionEditorComponent editor;
	private List<String> usedEndpointsPaths;
	private Set<String> serverContextPaths;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final Supplier<List<String>> authnOptionSupplier;
	private final Supplier<List<IdPInfo>> providersSupplier;
	private final URIAccessService uriAccessService;
	private final UnityServerConfiguration serverConfig;
	private final ImageAccessService imageAccessService;
	private final FileStorageService fileStorageService;

	public AttrIntrospectionServiceEditor(MessageSource msg, List<String> usedPaths, Set<String> serverContextPaths,
			AuthenticatorSupportService authenticatorSupportService, Supplier<List<String>> authnOptionSupplier,
			Supplier<List<IdPInfo>> providersSupplier, URIAccessService uriAccessService,
			UnityServerConfiguration serverConfig, ImageAccessService imageAccessService,
			FileStorageService fileStorageService)
	{
		this.msg = msg;
		this.usedEndpointsPaths = usedPaths;
		this.serverContextPaths = serverContextPaths;
		this.authenticatorSupportService = authenticatorSupportService;
		this.authnOptionSupplier = authnOptionSupplier;
		this.providersSupplier = providersSupplier;
		this.uriAccessService = uriAccessService;
		this.serverConfig = serverConfig;
		this.imageAccessService = imageAccessService;
		this.fileStorageService = fileStorageService;
	}

	@Override
	public ServiceEditorComponent getEditor(ServiceDefinition endpoint)
	{
		GeneralTab generalTab = new GeneralTab(msg, AttrIntrospectionEndpointFactory.TYPE, usedEndpointsPaths,
				serverContextPaths);
		AuthenticationOptionsTab authenticationOptionsTab = new AuthenticationOptionsTab(msg,
				authenticatorSupportService, authnOptionSupplier, uriAccessService, serverConfig);
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
			boolean hasErrors = serviceBinder.validate().hasErrors();
			hasErrors |= authnScreenConfigBinder.validate().hasErrors();
			hasErrors |= attrPoliciesBinder.validate().hasErrors();
			if (hasErrors)
			{
				setErrorInTabs();
				throw new FormValidationException();
			}

			DefaultServiceDefinition service = serviceBinder.getBean();
			VaadinEndpointProperties prop = new VaadinEndpointProperties(
					authnScreenConfigBinder.getBean().toProperties(msg, fileStorageService, service.getName()));
			try
			{
				service.setConfiguration(prop.getAsString() + "\n" + attrPoliciesBinder.getBean().toProperties(msg));
			} catch (JsonProcessingException e)
			{
				throw new FormValidationException();
			}
			return service;
		}
	}
}
