/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import io.imunity.vaadin.auth.authenticators.BaseLocalAuthenticatorEditor;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import org.springframework.beans.factory.ObjectFactory;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.util.Properties;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

@PrototypeComponent
class OTPAuthenticatorEditor extends BaseLocalAuthenticatorEditor implements AuthenticatorEditor
{
	private Binder<OTPConfigurationBean> configBinder;
	
	OTPAuthenticatorEditor(MessageSource msg, CredentialManagement credMan) throws EngineException
	{
		super(msg, credMan.getCredentialDefinitions().stream().filter(c -> c.getTypeId().equals(OTP.NAME))
				.map(CredentialDefinition::getName).collect(Collectors.toList()));
	}

	@Override
	public Component getEditor(AuthenticatorDefinition authenticator, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("OTPAuthenticatorEditor.defaultName"), authenticator,
				forceNameEditable);

		configBinder = new Binder<>(OTPConfigurationBean.class);
		
		FormLayout header = new FormLayout();
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));
		header.addFormItem(localCredential, msg.getMessage("BaseLocalAuthenticatorEditor.localCredential"));
		
		AccordionPanel interactiveLoginSettings = buildInteractiveLoginSettingsSection();
		interactiveLoginSettings.setWidthFull();

		VerticalLayout main = new VerticalLayout();
		main.setPadding(false);
		main.add(header, interactiveLoginSettings);

		OTPConfigurationBean config = new OTPConfigurationBean();
		if (editMode)
		{
			config.fromProperties(authenticator.configuration, msg);
		}
		configBinder.setBean(config);
	
		return main;
	}

	private AccordionPanel buildInteractiveLoginSettingsSection()
	{
		FormLayout interactiveLoginSettings = new FormLayout();
		interactiveLoginSettings.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		interactiveLoginSettings.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		
		LocalizedTextFieldDetails retrievalName = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		retrievalName.setWidth(TEXT_FIELD_MEDIUM.value());
		interactiveLoginSettings.addFormItem(retrievalName, msg.getMessage("OTPAuthenticatorEditor.formName"));
		configBinder.forField(retrievalName)
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.bind(OTPConfigurationBean::getRetrievalName, OTPConfigurationBean::setRetrievalName);

		return new AccordionPanel(msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"),
				interactiveLoginSettings);
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), OTP.NAME, getConfiguration(), getLocalCredential());
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

	public static class OTPConfigurationBean
	{
		private I18nString retrievalName = new I18nString();

		public OTPConfigurationBean()
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
						OTPRetrievalProperties.P + OTPRetrievalProperties.NAME, msg);
			}

			OTPRetrievalProperties prop = new OTPRetrievalProperties(raw);
			return prop.getAsString();
		}

		public void fromProperties(String properties, MessageSource msg)
		{
			Properties raw = UnityPropertiesHelper.parse(properties);
			OTPRetrievalProperties retrievalProperties = new OTPRetrievalProperties(raw);
			setRetrievalName(retrievalProperties.getLocalizedStringWithoutFallbackToDefault(msg, OTPRetrievalProperties.NAME));
		}
	}
	
	@org.springframework.stereotype.Component
	static class SMSAuthenticatorEditorFactory implements AuthenticatorEditorFactory
	{
		private final ObjectFactory<OTPAuthenticatorEditor> factory;

		SMSAuthenticatorEditorFactory(ObjectFactory<OTPAuthenticatorEditor> factory)
		{
			this.factory = factory;
		}

		@Override
		public String getSupportedAuthenticatorType()
		{
			return OTP.NAME;
		}

		@Override
		public AuthenticatorEditor createInstance() throws EngineException
		{
			return factory.getObject();
		}
	}
}
