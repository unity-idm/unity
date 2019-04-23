/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.authenticators.cert;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.credential.cert.CertificateVerificator;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.BaseLocalAuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.extensions.TLSRetrievalProperties;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * Certificate authenticator editor
 * 
 * @author P.Piernik
 *
 */
public class CertificateAuthenticatorEditor extends BaseLocalAuthenticatorEditor implements AuthenticatorEditor
{
	private UnityMessageSource msg;
	private Binder<CertConfiguration> configBinder;

	CertificateAuthenticatorEditor(UnityMessageSource msg, Collection<CredentialDefinition> credentialDefinitions)
			throws EngineException
	{
		super(msg, credentialDefinitions.stream().filter(c -> c.getTypeId().equals(CertificateVerificator.NAME))
				.map(c -> c.getName()).collect(Collectors.toList()));
		this.msg = msg;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("CertificateAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(CertConfiguration.class);

		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);
		header.addComponent(name);
		header.addComponent(localCredential);

		CollapsibleLayout interactiveLoginSettings = buildInteractiveLoginSettingsSection();

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(header);
		main.addComponent(interactiveLoginSettings);

		CertConfiguration config = new CertConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
		}
		configBinder.setBean(config);

		return main;
	}

	private CollapsibleLayout buildInteractiveLoginSettingsSection()
	{
		FormLayoutWithFixedCaptionWidth interactiveLoginSettings = new FormLayoutWithFixedCaptionWidth();
		interactiveLoginSettings.setMargin(false);
		I18nTextField retrivalName = new I18nTextField(msg);
		retrivalName.setCaption(msg.getMessage("CertificateAuthenticatorEditor.formName"));
		TextField logoURL = new TextField();
		logoURL.setCaption(msg.getMessage("CertificateAuthenticatorEditor.logoURL"));
		interactiveLoginSettings.addComponents(retrivalName, logoURL);
		CollapsibleLayout wrapper = new CollapsibleLayout(
				msg.getMessage("CertificateAuthenticatorEditor.interactiveLoginSettings"),
				interactiveLoginSettings);
		configBinder.forField(retrivalName).bind("retrivalName");
		configBinder.forField(logoURL).bind("logoURL");

		return wrapper;
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException
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
			return configBinder.getBean().toProperties();
		} catch (ConfigurationException e)
		{
			throw new FormValidationException("Invalid configuration of the certificate verificator", e);
		}
	}

	public static class CertConfiguration
	{
		private I18nString retrivalName;
		private String logoURL;

		public CertConfiguration()
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

			if (retrivalName != null)
			{
				retrivalName.toProperties(raw,
						TLSRetrievalProperties.P + TLSRetrievalProperties.NAME + ".");
			}

			if (logoURL != null && !logoURL.isEmpty())
			{
				raw.put(TLSRetrievalProperties.P + TLSRetrievalProperties.LOGO_URL, logoURL);
			}

			TLSRetrievalProperties prop = new TLSRetrievalProperties(raw);
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
				throw new InternalException("Invalid configuration of the certificate verificator", e);
			}

			TLSRetrievalProperties certRetrievalProperties = new TLSRetrievalProperties(raw);
			retrivalName = certRetrievalProperties.getLocalizedString(msg, TLSRetrievalProperties.NAME);
			logoURL = certRetrievalProperties.getValue(TLSRetrievalProperties.LOGO_URL);
		}

	}
}
