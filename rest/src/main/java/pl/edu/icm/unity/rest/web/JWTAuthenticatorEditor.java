/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.BaseAuthenticatorEditor;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.rest.jwt.JWTAuthenticationProperties;
import pl.edu.icm.unity.rest.jwt.authn.JWTVerificator;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.Set;

import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;


class JWTAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private final Set<String> credentials;
	private Binder<JWTConfiguration> configBinder;

	JWTAuthenticatorEditor(MessageSource msg, Set<String> credentials)
	{
		super(msg);
		this.credentials = credentials;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("JWTAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		FormLayout formLayout = new FormLayout();
		formLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		formLayout.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));

		Select<String> credential = new Select<>();
		formLayout.addFormItem(credential, msg.getMessage("JWTAuthenticatorEditor.signingCredential"));
		credential.setItems(credentials);

		IntegerField ttl = new IntegerField();
		ttl.setMin(0);
		formLayout.addFormItem(ttl, msg.getMessage("JWTAuthenticatorEditor.tokenTTL"));

		configBinder = new Binder<>(JWTConfiguration.class);
		configBinder.forField(credential).asRequired(msg.getMessage("fieldRequired")).bind("credential");
		configBinder.forField(ttl).asRequired(msg.getMessage("fieldRequired"))
				.bind("ttl");


		VerticalLayout main = new VerticalLayout();
		main.setPadding(false);
		main.add(formLayout);
		
		JWTConfiguration config = new JWTConfiguration();
		if (!credentials.isEmpty())
		{
			config.setCredential(credentials.iterator().next());
		}

		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
		}

		configBinder.setBean(config);

		return main;
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), JWTVerificator.NAME, getConfiguration(), null);
	}

	private String getConfiguration() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();

		try
		{
			return configBinder.getBean().toProperties();
		} catch (ConfigurationException e)
		{
			throw new FormValidationException("Invalid configuration of the jwt verificator", e);
		}
	}

	public static class JWTConfiguration
	{
		private String credential;
		private int ttl;

		public JWTConfiguration()
		{
			setTtl(JWTAuthenticationProperties.DEFAULT_TOKEN_TTL);
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

			raw.put(JWTAuthenticationProperties.PREFIX + JWTAuthenticationProperties.SIGNING_CREDENTIAL,
					credential);
			raw.put(JWTAuthenticationProperties.PREFIX + JWTAuthenticationProperties.TOKEN_TTL,
					String.valueOf(ttl));

			JWTAuthenticationProperties prop = new JWTAuthenticationProperties(raw);
			return prop.getAsString();

		}

		public void fromProperties(String properties, MessageSource msg)
		{
			Properties raw = new Properties();
			try
			{
				raw.load(new StringReader(properties));
			} catch (IOException e)
			{
				throw new InternalException("Invalid configuration of the jwt verificator", e);
			}

			JWTAuthenticationProperties jwtProp = new JWTAuthenticationProperties(raw);

			credential = jwtProp.getValue(JWTAuthenticationProperties.SIGNING_CREDENTIAL);
			ttl = jwtProp.getIntValue(JWTAuthenticationProperties.TOKEN_TTL);
		}
	}
}
