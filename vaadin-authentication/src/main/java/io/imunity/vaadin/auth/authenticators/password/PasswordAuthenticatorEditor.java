/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.authenticators.password;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.BaseLocalAuthenticatorEditor;
import io.imunity.vaadin.auth.extensions.PasswordRetrievalProperties;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;


class PasswordAuthenticatorEditor extends BaseLocalAuthenticatorEditor implements AuthenticatorEditor
{
	private final MessageSource msg;
	private Binder<PasswordConfiguration> configBinder;

	PasswordAuthenticatorEditor(MessageSource msg, Collection<CredentialDefinition> credentialDefinitions)
	{
		super(msg, credentialDefinitions.stream().filter(c -> c.getTypeId().equals(PasswordVerificator.NAME))
				.map(CredentialDefinition::getName).collect(Collectors.toList()));
		this.msg = msg;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("PasswordAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);
		configBinder = new Binder<>(PasswordConfiguration.class);

		FormLayout header = new FormLayout();
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));
		header.addFormItem(localCredential, msg.getMessage("BaseLocalAuthenticatorEditor.localCredential"));

		AccordionPanel interactiveLoginSettings = buildInteractiveLoginSettingsSection();
		interactiveLoginSettings.setWidthFull();

		VerticalLayout main = new VerticalLayout();
		main.setPadding(false);
		main.add(header);
		main.add(interactiveLoginSettings);
		
		PasswordConfiguration config = new PasswordConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
		} else
		{
			config.setLocalCredential(getDefaultLocalCredential());
		}
		configBinder.setBean(config);
		
		return main;
	}

	private AccordionPanel buildInteractiveLoginSettingsSection()
	{
		FormLayout formLayout = new FormLayout();
		formLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		LocalizedTextFieldDetails retrievalName = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		configBinder.forField(retrievalName).bind(configuration -> configuration.getRetrievalName().getLocalizedMap(),
				(configuration, value) -> configuration.setRetrievalName(new I18nString(value)));
		formLayout.addFormItem(retrievalName, msg.getMessage("PasswordAuthenticatorEditor.passwordName"));
		
		return new AccordionPanel(msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"), formLayout);
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), PasswordVerificator.NAME, getConfiguration(),
				getLocalCredential());

	}

	private String getConfiguration() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();

		try
		{
			return configBinder.getBean().toProperties(msg);
		} catch (ConfigurationException e)
		{
			throw new FormValidationException("Invalid configuration of the password verificator", e);
		}
	}

	public static class PasswordConfiguration
	{
		private I18nString retrievalName = new I18nString();
		private String localCredential;

		public PasswordConfiguration()
		{
		}

		public I18nString getRetrievalName()
		{
			return retrievalName;
		}

		public void setRetrievalName(I18nString retrivalName)
		{
			this.retrievalName = retrivalName;
		}

		public String getLocalCredential()
		{
			return localCredential;
		}

		public void setLocalCredential(String localCredential)
		{
			this.localCredential = localCredential;
		}

		private String toProperties(MessageSource msg)
		{
			Properties raw = new Properties();
			if (getRetrievalName() != null)
			{
				getRetrievalName().toProperties(raw,
						PasswordRetrievalProperties.P + PasswordRetrievalProperties.NAME, msg);
			}
			PasswordRetrievalProperties prop = new PasswordRetrievalProperties(raw);
			return prop.getAsString();
		}

		private void fromProperties(String properties, MessageSource msg)
		{
			Properties raw = new Properties();
			try
			{
				raw.load(new StringReader(properties == null ? "" : properties));
			} catch (IOException e)
			{
				throw new InternalException("Invalid configuration of the password verificator", e);
			}

			PasswordRetrievalProperties passwordRetrievalProperties = new PasswordRetrievalProperties(raw);
			setRetrievalName(passwordRetrievalProperties.getLocalizedStringWithoutFallbackToDefault(msg,
					PasswordRetrievalProperties.NAME));
		}

	}
}
