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
import pl.edu.icm.unity.restadm.RESTAdminEndpoint;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.webui.authn.services.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;

/**
 * 
 * @author P.Piernik
 *
 */
public class RestAdminServiceEditorComponent extends ServiceEditorBase
{
	private Binder<RestAdminConfiguration> binder;

	public RestAdminServiceEditorComponent(UnityMessageSource msg, ServiceDefinition toEdit, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators)
	{
		super(msg, RESTAdminEndpoint.TYPE, toEdit, allRealms, flows, authenticators);

		RestAdminConfiguration config = new RestAdminConfiguration();
		if (toEdit != null && toEdit.getConfiguration() != null)
		{
			config.fromProperties(toEdit.getConfiguration(), msg);
		}
		binder = new Binder<>(RestAdminConfiguration.class);
		addToGeneralTab(buildCorsSection());
		binder.setBean(config);

	}

	private Component buildCorsSection()
	{

		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		ChipsWithTextfield allowedCORSheaders = new ChipsWithTextfield(msg);
		allowedCORSheaders.setCaption(msg.getMessage("RestAdminServiceEditorComponent.allowedCORSheaders"));
		binder.forField(allowedCORSheaders).bind("allowedCORSheaders");
		main.addComponent(allowedCORSheaders);

		ChipsWithTextfield allowedCORSorigins = new ChipsWithTextfield(msg);
		allowedCORSorigins.setCaption(msg.getMessage("RestAdminServiceEditorComponent.allowedCORSorigins"));
		main.addComponent(allowedCORSorigins);
		binder.forField(allowedCORSorigins).bind("allowedCORSorigins");

		CollapsibleLayout corsSection = new CollapsibleLayout(
				msg.getMessage("RestAdminServiceEditorComponent.cors"), main);
		return corsSection;
	}

	@Override
	protected String getConfiguration(String serviceName) throws FormValidationException
	{
		validateConfiguration();
		return binder.getBean().toProperties();

	}

	@Override
	protected void validateConfiguration() throws FormValidationException
	{
		if (binder.validate().hasErrors())
		{
			throw new FormValidationException();
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
