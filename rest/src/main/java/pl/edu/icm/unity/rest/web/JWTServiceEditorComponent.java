/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.web;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.rest.RESTEndpointProperties;
import pl.edu.icm.unity.rest.jwt.JWTAuthenticationProperties;
import pl.edu.icm.unity.rest.jwt.endpoint.JWTManagementEndpoint;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.webui.authn.endpoints.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.endpoints.ServiceEditorBase;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;

/**
 * 
 * @author P.Piernik
 *
 */
public class JWTServiceEditorComponent extends ServiceEditorBase
{
	private Binder<JWTConfiguration> binder;
	private Set<String> credentials;

	public JWTServiceEditorComponent(UnityMessageSource msg, ServiceDefinition toEdit, List<String> allRealms,
			List<AuthenticationFlowDefinition> flows, List<AuthenticatorInfo> authenticators,
			Set<String> credentials)
	{
		super(msg, JWTManagementEndpoint.TYPE, toEdit, allRealms, flows, authenticators);
		this.credentials = credentials;

		JWTConfiguration config = new JWTConfiguration();
		if (toEdit != null && toEdit.getConfiguration() != null)
		{
			config.fromProperties(toEdit.getConfiguration(), msg);
		}
		binder = new Binder<>(JWTConfiguration.class);
		addToGeneralTab(buildCorsTab());
		addToGeneralTab(buildJWTtab());
		binder.setBean(config);

	}

	private Component buildCorsTab()
	{

		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		ChipsWithTextfield allowedCORSheaders = new ChipsWithTextfield(msg);
		allowedCORSheaders.setCaption(msg.getMessage("JWTServiceEditorComponent.allowedCORSheaders"));
		binder.forField(allowedCORSheaders).bind("allowedCORSheaders");
		main.addComponent(allowedCORSheaders);

		ChipsWithTextfield allowedCORSorigins = new ChipsWithTextfield(msg);
		allowedCORSorigins.setCaption(msg.getMessage("JWTServiceEditorComponent.allowedCORSorigins"));
		main.addComponent(allowedCORSorigins);
		binder.forField(allowedCORSorigins).bind("allowedCORSorigins");

		CollapsibleLayout corsSection = new CollapsibleLayout(msg.getMessage("JWTServiceEditorComponent.cors"),
				main);
		return corsSection;
	}

	private Component buildJWTtab()
	{
		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(false);

		ComboBox<String> credential = new ComboBox<>();
		credential.setId("cre");
		credential.setCaption(msg.getMessage("JWTServiceEditorComponent.signingCredential"));
		credential.setEmptySelectionAllowed(false);
		credential.setItems(credentials);
		binder.forField(credential).asRequired(msg.getMessage("fieldRequired")).bind("credential");
		main.addComponent(credential);

		TextField ttl = new TextField();
		ttl.setCaption(msg.getMessage("JWTServiceEditorComponent.tokenTTL"));
		binder.forField(ttl).asRequired(msg.getMessage("fieldRequired"))
				.withConverter(new StringToIntegerConverter(msg.getMessage("notAPositiveNumber")))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 0, null))
				.bind("ttl");
		main.addComponent(ttl);

		CollapsibleLayout jwtSection = new CollapsibleLayout(msg.getMessage("JWTServiceEditorComponent.jwt"),
				main);
		jwtSection.expand();
		return jwtSection;
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

	public class JWTConfiguration

	{
		private List<String> allowedCORSheaders;
		private List<String> allowedCORSorigins;
		private String credential;
		private int ttl;

		public JWTConfiguration()
		{
			allowedCORSheaders = new ArrayList<>();
			allowedCORSorigins = new ArrayList<>();
			setTtl(JWTAuthenticationProperties.DEFAULT_TOKEN_TTL);
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

		public String getCredential()
		{
			return credential;
		}

		public void setCredential(String credential)
		{
			this.credential = credential;
		}

		public int getTtl()
		{
			return ttl;
		}

		public void setTtl(int ttl)
		{
			this.ttl = ttl;
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

			raw.put(JWTAuthenticationProperties.PREFIX + JWTAuthenticationProperties.SIGNING_CREDENTIAL,
					credential);
			raw.put(JWTAuthenticationProperties.PREFIX + JWTAuthenticationProperties.TOKEN_TTL,
					String.valueOf(ttl));

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

			JWTAuthenticationProperties jwtProp = new JWTAuthenticationProperties(raw);
			credential = jwtProp.getValue(JWTAuthenticationProperties.SIGNING_CREDENTIAL);
			ttl = jwtProp.getIntValue(JWTAuthenticationProperties.TOKEN_TTL);

		}

	}
}
