/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.v8;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.otp.OTP;
import io.imunity.otp.OTPRetrievalProperties;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;
import pl.edu.icm.unity.webui.authn.authenticators.BaseLocalAuthenticatorEditor;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

import java.util.Properties;
import java.util.stream.Collectors;

@PrototypeComponent
class OTPAuthenticatorEditorV8 extends BaseLocalAuthenticatorEditor implements AuthenticatorEditor
{
	private Binder<OTPConfigurationBean> configBinder;
	
	@Autowired
	OTPAuthenticatorEditorV8(MessageSource msg, CredentialManagement credMan) throws EngineException
	{
		super(msg, credMan.getCredentialDefinitions().stream().filter(c -> c.getTypeId().equals(OTP.NAME))
				.map(c -> c.getName()).collect(Collectors.toList()));
	}

	@Override
	public Component getEditor(AuthenticatorDefinition authenticator, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("OTPAuthenticatorEditor.defaultName"), authenticator,
				forceNameEditable);

		configBinder = new Binder<>(OTPConfigurationBean.class);
		
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);
		header.addComponent(name);
		header.addComponent(localCredential);
		
		CollapsibleLayout interactiveLoginSettings = buildInteractiveLoginSettingsSection();
	
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(header);
		main.addComponent(interactiveLoginSettings);

		OTPConfigurationBean config = new OTPConfigurationBean();
		if (editMode)
		{
			config.fromProperties(authenticator.configuration, msg);
		}
		configBinder.setBean(config);
	
		return main;
	}

	private CollapsibleLayout buildInteractiveLoginSettingsSection()
	{
		FormLayoutWithFixedCaptionWidth interactiveLoginSettings = new FormLayoutWithFixedCaptionWidth();
		interactiveLoginSettings.setMargin(false);
		
		I18nTextField retrievalName = new I18nTextField(msg);
		retrievalName.setCaption(msg.getMessage("OTPAuthenticatorEditor.formName"));
		configBinder.forField(retrievalName).bind("retrievalName");

		interactiveLoginSettings.addComponents(retrievalName);
		CollapsibleLayout wrapper = new CollapsibleLayout(
				msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"),
				interactiveLoginSettings);
		return wrapper;
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException
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
		private I18nString retrievalName;

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
		private ObjectFactory<OTPAuthenticatorEditorV8> factory;

		@Autowired
		SMSAuthenticatorEditorFactory(ObjectFactory<OTPAuthenticatorEditorV8> factory)
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
