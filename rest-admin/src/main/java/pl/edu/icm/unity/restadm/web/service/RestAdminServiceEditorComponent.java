/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.web.service;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.rest.RESTEndpointProperties;
import pl.edu.icm.unity.rest.jwt.endpoint.JWTManagementEndpoint;
import pl.edu.icm.unity.restadm.RESTAdminEndpoint;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.authn.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.authn.services.tabs.AuthenticationTab;
import pl.edu.icm.unity.webui.authn.services.tabs.GeneralTab;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;

/**
 * Rest admin service editor component
 * 
 * @author P.Piernik
 *
 */
public class RestAdminServiceEditorComponent extends ServiceEditorBase
{
	private Binder<RestAdminConfiguration> restBinder;
	private Binder<DefaultServiceDefinition> serviceBinder;

	public RestAdminServiceEditorComponent(UnityMessageSource msg, DefaultServiceDefinition toEdit,
			List<String> allRealms, List<AuthenticationFlowDefinition> flows,
			List<AuthenticatorInfo> authenticators, List<String> usedPaths)
	{
		super(msg);
		boolean editMode = toEdit != null;

		restBinder = new Binder<>(RestAdminConfiguration.class);
		serviceBinder = new Binder<>(DefaultServiceDefinition.class);

		registerTab(new RestAdminGeneralTab(msg, serviceBinder, RESTAdminEndpoint.TYPE, usedPaths, editMode));
		registerTab(new AuthenticationTab(msg, flows, authenticators, allRealms,
				JWTManagementEndpoint.TYPE.getSupportedBinding(), serviceBinder));
		serviceBinder.setBean(
				editMode ? toEdit : new DefaultServiceDefinition(RESTAdminEndpoint.TYPE.getName()));

		RestAdminConfiguration config = new RestAdminConfiguration();
		if (editMode && toEdit.getConfiguration() != null)
		{
			config.fromProperties(toEdit.getConfiguration(), msg);
		}

		restBinder.setBean(config);

	}

	public ServiceDefinition getServiceDefiniton() throws FormValidationException
	{
		boolean hasErrors = serviceBinder.validate().hasErrors();
		hasErrors |= restBinder.validate().hasErrors();
		if (hasErrors)
		{
			setErrorInTabs();
			throw new FormValidationException();
		}

		DefaultServiceDefinition service = serviceBinder.getBean();
		service.setConfiguration(restBinder.getBean().toProperties());
		return service;
	}

	public class RestAdminGeneralTab extends GeneralTab
	{
		public RestAdminGeneralTab(UnityMessageSource msg, Binder<DefaultServiceDefinition> serviceBinder,
				EndpointTypeDescription type, List<String> usedPaths, boolean editMode)
		{
			super(msg, serviceBinder, type, usedPaths, editMode);
			initUI();
		}

		private void initUI()
		{
			mainLayout.addComponent(buildCorsSection());
		}

		private Component buildCorsSection()
		{

			FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
			main.setMargin(false);

			ChipsWithTextfield allowedCORSheaders = new ChipsWithTextfield(msg);
			allowedCORSheaders.setCaption(
					msg.getMessage("RestAdminServiceEditorComponent.allowedCORSheaders"));
			restBinder.forField(allowedCORSheaders).bind("allowedCORSheaders");
			main.addComponent(allowedCORSheaders);

			ChipsWithTextfield allowedCORSorigins = new ChipsWithTextfield(msg);
			allowedCORSorigins.setCaption(
					msg.getMessage("RestAdminServiceEditorComponent.allowedCORSorigins"));
			main.addComponent(allowedCORSorigins);
			restBinder.forField(allowedCORSorigins).bind("allowedCORSorigins");

			CollapsibleLayout corsSection = new CollapsibleLayout(
					msg.getMessage("RestAdminServiceEditorComponent.cors"), main);
			return corsSection;
		}
	}

	public class RestAdminConfiguration

	{
		private List<String> allowedCORSheaders;
		private List<String> allowedCORSorigins;

		public RestAdminConfiguration()
		{
			allowedCORSheaders = new ArrayList<>();
			allowedCORSorigins = new ArrayList<>();
		}

		public List<String> getAllowedCORSheaders()
		{
			return allowedCORSheaders;
		}

		public void setAllowedCORSheaders(List<String> allowedCORSheaders)
		{
			this.allowedCORSheaders = allowedCORSheaders;
		}

		public void setAllowedCORSorigins(List<String> allowedCORSorigins)
		{
			this.allowedCORSorigins = allowedCORSorigins;
		}

		public List<String> getAllowedCORSorigins()
		{
			return allowedCORSorigins;
		}

		public String toProperties()
		{
			Properties raw = new Properties();

			getAllowedCORSheaders().forEach(a -> {

				int i = getAllowedCORSheaders().indexOf(a) + 1;
				raw.put(RESTEndpointProperties.PREFIX + RESTEndpointProperties.ENABLED_CORS_HEADERS + i,
						a);
			});

			getAllowedCORSorigins().forEach(a -> {

				int i = getAllowedCORSorigins().indexOf(a) + 1;
				raw.put(RESTEndpointProperties.PREFIX + RESTEndpointProperties.ENABLED_CORS_ORIGINS + i,
						a);
			});

			RESTEndpointProperties prop = new RESTEndpointProperties(raw);
			return prop.getAsString();
		}

		public void fromProperties(String properties, UnityMessageSource msg)
		{
			Properties raw = new Properties();
			try
			{
				raw.load(new StringReader(properties));
			} catch (IOException e)
			{
				throw new InternalException("Invalid configuration of the rest admin service", e);
			}

			RESTEndpointProperties restAdminProp = new RESTEndpointProperties(raw);
			allowedCORSheaders = restAdminProp.getListOfValues(RESTEndpointProperties.ENABLED_CORS_HEADERS);
			allowedCORSorigins = restAdminProp.getListOfValues(RESTEndpointProperties.ENABLED_CORS_ORIGINS);

		}

	}
}
