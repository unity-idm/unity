/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.authenticators.sms;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.BaseLocalAuthenticatorEditor;
import io.imunity.vaadin.auth.extensions.SMSRetrievalProperties;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.stdext.credential.sms.SMSVerificator;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;


class SMSAuthenticatorEditor extends BaseLocalAuthenticatorEditor implements AuthenticatorEditor
{
	private final MessageSource msg;
	private Binder<SMSConfiguration> configBinder;

	SMSAuthenticatorEditor(MessageSource msg, Collection<CredentialDefinition> credentialDefinitions)
	{
		super(msg, credentialDefinitions.stream().filter(c -> c.getTypeId().equals(SMSVerificator.NAME))
				.map(CredentialDefinition::getName).collect(Collectors.toList()));
		this.msg = msg;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("SMSAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(SMSConfiguration.class);

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

		SMSConfiguration config = new SMSConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
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
			(configuration, value) -> configuration.setRetrievalName(new I18nString(value)));;
		formLayout.addFormItem(retrievalName, msg.getMessage("SMSAuthenticatorEditor.formName"));

		return new AccordionPanel(msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"), formLayout);
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), SMSVerificator.NAME, getConfiguration(),
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
			throw new FormValidationException("Invalid configuration of the sms verificator", e);
		}
	}

	public static class SMSConfiguration
	{
		private I18nString retrievalName = new I18nString();

		public SMSConfiguration()
		{
		}

		public I18nString getRetrievalName()
		{
			return retrievalName;
		}

		public void setRetrievalName(I18nString retrievalName)
		{
			this.retrievalName = retrievalName;
		}

		public String toProperties(MessageSource msg)
		{
			Properties raw = new Properties();

			if (getRetrievalName() != null)
			{
				getRetrievalName().toProperties(raw,
						SMSRetrievalProperties.P + SMSRetrievalProperties.NAME, msg);
			}

			SMSRetrievalProperties prop = new SMSRetrievalProperties(raw);
			return prop.getAsString();
		}

		public void fromProperties(String properties, MessageSource msg)
		{
			Properties raw = UnityPropertiesHelper.parse(properties);
			SMSRetrievalProperties smsRetrievalProperties = new SMSRetrievalProperties(raw);
			setRetrievalName(smsRetrievalProperties.getLocalizedStringWithoutFallbackToDefault(msg, SMSRetrievalProperties.NAME));
		}
	}
}
