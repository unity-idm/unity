/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.fido.service.FidoCredentialVerificator;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import io.imunity.vaadin.auth.authenticators.BaseLocalAuthenticatorEditor;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
class FidoAuthenticatorEditor extends BaseLocalAuthenticatorEditor implements AuthenticatorEditor
{
	private Binder<FidoConfigurationBean> configBinder;
	
	FidoAuthenticatorEditor(MessageSource msg, CredentialManagement credMan) throws EngineException
	{
		super(msg, credMan.getCredentialDefinitions().stream().filter(c -> c.getTypeId().equals(FidoCredentialVerificator.NAME))
				.map(CredentialDefinition::getName).collect(Collectors.toList()));
	}

	@Override
	public Component getEditor(AuthenticatorDefinition authenticator, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("FidoAuthenticatorEditor.defaultName"), authenticator,
				forceNameEditable);

		configBinder = new Binder<>(FidoConfigurationBean.class);

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

		FidoConfigurationBean config = new FidoConfigurationBean();
		if (editMode)
		{
			config.fromProperties(authenticator.configuration, msg);
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
		retrievalName.setWidth(TEXT_FIELD_MEDIUM.value());
		configBinder.forField(retrievalName).bind(configuration -> configuration.getRetrievalName().getLocalizedMap(),
				(configuration, value) -> configuration.setRetrievalName(new I18nString(value)));
		formLayout.addFormItem(retrievalName, msg.getMessage("FidoAuthenticatorEditor.formName"));

		return new AccordionPanel(msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"), formLayout);
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), FidoCredentialVerificator.NAME, getConfiguration(), getLocalCredential());
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
			throw new FormValidationException("Invalid configuration of the fido verificator", e);
		}
	}

	public static class FidoConfigurationBean
	{
		private I18nString retrievalName = new I18nString();

		public FidoConfigurationBean()
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
						FidoRetrievalProperties.P + FidoRetrievalProperties.NAME, msg);
			}

			FidoRetrievalProperties prop = new FidoRetrievalProperties(raw);
			return prop.getAsString();
		}

		public void fromProperties(String properties, MessageSource msg)
		{
			Properties raw = UnityPropertiesHelper.parse(properties);
			FidoRetrievalProperties retrievalProperties = new FidoRetrievalProperties(raw);
			setRetrievalName(retrievalProperties.getLocalizedStringWithoutFallbackToDefault(msg, FidoRetrievalProperties.NAME));
		}
	}
	
	@org.springframework.stereotype.Component
	static class FidoAuthenticatorEditorFactory implements AuthenticatorEditorFactory
	{
		private final ObjectFactory<FidoAuthenticatorEditor> factory;

		@Autowired
		FidoAuthenticatorEditorFactory(ObjectFactory<FidoAuthenticatorEditor> factory)
		{
			this.factory = factory;
		}

		@Override
		public String getSupportedAuthenticatorType()
		{
			return FidoCredentialVerificator.NAME;
		}

		@Override
		public AuthenticatorEditor createInstance()
		{
			return factory.getObject();
		}
	}
}
