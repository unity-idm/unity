/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.authenticators.sms;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.credential.sms.SMSVerificator;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.BaseLocalAuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.extensions.SMSRetrievalProperties;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * SMS authenticator editor
 * 
 * @author P.Piernik
 *
 */
public class SMSAuthenticatorEditor extends BaseLocalAuthenticatorEditor implements AuthenticatorEditor
{
	private UnityMessageSource msg;
	private Binder<SMSConfiguration> configBinder;

	SMSAuthenticatorEditor(UnityMessageSource msg, Collection<CredentialDefinition> credentialDefinitions)
			throws EngineException
	{
		super(msg, credentialDefinitions.stream().filter(c -> c.getTypeId().equals(SMSVerificator.NAME))
				.map(c -> c.getName()).collect(Collectors.toList()));
		this.msg = msg;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("SMSAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(SMSConfiguration.class);

		CollapsibleLayout interactiveLoginSettings = buildInteractiveLoginSettingsSection();

		SMSConfiguration config = new SMSConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
		}
		configBinder.setBean(config);

		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);
		header.addComponent(name);
		header.addComponent(localCredential);

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(header);
		main.addComponent(interactiveLoginSettings);

		return main;
	}

	private CollapsibleLayout buildInteractiveLoginSettingsSection()
	{
		FormLayoutWithFixedCaptionWidth interactiveLoginSettings = new FormLayoutWithFixedCaptionWidth();
		interactiveLoginSettings.setMargin(false);
		I18nTextField retrivalName = new I18nTextField(msg);
		retrivalName.setCaption(msg.getMessage("SMSAuthenticatorEditor.formName"));
		TextField logoURL = new TextField();
		logoURL.setCaption(msg.getMessage("SMSAuthenticatorEditor.logoURL"));
		interactiveLoginSettings.addComponents(retrivalName, logoURL);
		CollapsibleLayout wrapper = new CollapsibleLayout(
				msg.getMessage("SMSAuthenticatorEditor.interactiveLoginSettings"),
				interactiveLoginSettings);
		configBinder.forField(retrivalName).bind("retrivalName");
		configBinder.forField(logoURL).bind("logoURL");

		return wrapper;
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), SMSVerificator.NAME, getConfiguration(),
				getLocalCredential());

	}

	private String getConfiguration() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();

		return configBinder.getBean().toProperties();
	}

	public static class SMSConfiguration
	{
		private I18nString retrivalName;
		private String localCredential;
		private String logoURL;

		public SMSConfiguration()
		{
		}

		public I18nString getRetrivalName()
		{
			return retrivalName;
		}

		public void setRetrivalName(I18nString retrivalName)
		{
			this.retrivalName = retrivalName;
		}

		public String getLocalCredential()
		{
			return localCredential;
		}

		public void setLocalCredential(String localCredential)
		{
			this.localCredential = localCredential;
		}

		public String getLogoURL()
		{
			return logoURL;
		}

		public void setLogoURL(String logoURL)
		{
			this.logoURL = logoURL;
		}

		public String toProperties()
		{
			Properties raw = new Properties();
			if (retrivalName != null && !retrivalName.getMap().isEmpty())
			{
				for (Map.Entry<String, String> entry : retrivalName.getMap().entrySet())
				{
					raw.put(SMSRetrievalProperties.P + SMSRetrievalProperties.NAME + "."
							+ entry.getKey(), entry.getValue());
				}
			}

			if (logoURL != null && !logoURL.isEmpty())
			{
				raw.put(SMSRetrievalProperties.P + SMSRetrievalProperties.LOGO_URL, logoURL);
			}

			SMSRetrievalProperties prop = new SMSRetrievalProperties(raw);
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
				throw new InternalException("Invalid configuration of the sms verificator", e);
			}

			SMSRetrievalProperties passwordRetrievalProperties = new SMSRetrievalProperties(raw);
			retrivalName = passwordRetrievalProperties.getLocalizedString(msg, SMSRetrievalProperties.NAME);
			logoURL = passwordRetrievalProperties.getValue(SMSRetrievalProperties.LOGO_URL);
		}

	}
}
