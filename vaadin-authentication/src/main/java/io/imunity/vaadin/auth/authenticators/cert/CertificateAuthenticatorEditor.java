/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.authenticators.cert;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.BaseLocalAuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.SubViewSwitcher;
import io.imunity.vaadin.auth.extensions.TLSRetrievalProperties;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.stdext.credential.cert.CertificateVerificator;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;


class CertificateAuthenticatorEditor extends BaseLocalAuthenticatorEditor implements AuthenticatorEditor
{
	private MessageSource msg;
	private Binder<CertConfiguration> configBinder;

	CertificateAuthenticatorEditor(MessageSource msg, Collection<CredentialDefinition> credentialDefinitions)
	{
		super(msg, credentialDefinitions.stream().filter(c -> c.getTypeId().equals(CertificateVerificator.NAME))
				.map(CredentialDefinition::getName).collect(Collectors.toList()));
		this.msg = msg;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("CertificateAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(CertConfiguration.class);

		FormLayout header = new FormLayout();
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));
		header.addFormItem(localCredential, msg.getMessage("BaseLocalAuthenticatorEditor.localCredential"));

		Accordion interactiveLoginSettings = buildInteractiveLoginSettingsSection();

		VerticalLayout main = new VerticalLayout();
		main.setPadding(false);
		main.add(header);
		main.add(interactiveLoginSettings);

		CertConfiguration config = new CertConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
		}
		configBinder.setBean(config);

		return main;
	}

	private Accordion buildInteractiveLoginSettingsSection()
	{
		Accordion interactiveLoginSettings = new Accordion();
		FormLayout formLayout = new FormLayout();
		formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		LocalizedTextFieldDetails retrievalName = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(),
				msg.getLocale());
		configBinder.forField(retrievalName)
				.bind(configuration -> configuration.getRetrievalName().getLocalizedMap(),
						(configuration, value) -> configuration.setRetrievalName(new I18nString(value)));
		formLayout.addFormItem(retrievalName, msg.getMessage("CertificateAuthenticatorEditor.displayedName"));

		interactiveLoginSettings.add(msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"), formLayout);
		interactiveLoginSettings.close();
		return interactiveLoginSettings;
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), CertificateVerificator.NAME, getConfiguration(),
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
			throw new FormValidationException("Invalid configuration of the certificate verificator", e);
		}
	}

	public static class CertConfiguration
	{
		private I18nString retrievalName;

		public CertConfiguration()
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
						TLSRetrievalProperties.P + TLSRetrievalProperties.NAME, msg);
			}

			TLSRetrievalProperties prop = new TLSRetrievalProperties(raw);
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
				throw new InternalException("Invalid configuration of the certificate verificator", e);
			}

			TLSRetrievalProperties certRetrievalProperties = new TLSRetrievalProperties(raw);
			setRetrievalName(certRetrievalProperties.getLocalizedStringWithoutFallbackToDefault(msg,
					TLSRetrievalProperties.NAME));
		}
	}
}
