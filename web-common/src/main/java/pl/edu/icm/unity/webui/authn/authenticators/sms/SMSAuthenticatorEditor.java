/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.authenticators.sms;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.FileStorageService.StandardOwner;
import pl.edu.icm.unity.engine.api.files.URIHelper;
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
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.file.FileFieldUtils;
import pl.edu.icm.unity.webui.common.file.LogoFileField;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * SMS authenticator editor
 * 
 * @author P.Piernik
 *
 */
class SMSAuthenticatorEditor extends BaseLocalAuthenticatorEditor implements AuthenticatorEditor
{
	private UnityMessageSource msg;
	private FileStorageService fileStorageService;
	private Binder<SMSConfiguration> configBinder;

	SMSAuthenticatorEditor(UnityMessageSource msg, FileStorageService fileStorageService, Collection<CredentialDefinition> credentialDefinitions)
			throws EngineException
	{
		super(msg, credentialDefinitions.stream().filter(c -> c.getTypeId().equals(SMSVerificator.NAME))
				.map(c -> c.getName()).collect(Collectors.toList()));
		this.msg = msg;
		this.fileStorageService = fileStorageService;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("SMSAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(SMSConfiguration.class);
		
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);
		header.addComponent(name);
		header.addComponent(localCredential);
		
		CollapsibleLayout interactiveLoginSettings = buildInteractiveLoginSettingsSection();
	
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(header);
		main.addComponent(interactiveLoginSettings);

		SMSConfiguration config = new SMSConfiguration(fileStorageService);
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg, fileStorageService);
		}
		configBinder.setBean(config);
	
		return main;
	}

	private CollapsibleLayout buildInteractiveLoginSettingsSection()
	{
		FormLayoutWithFixedCaptionWidth interactiveLoginSettings = new FormLayoutWithFixedCaptionWidth();
		interactiveLoginSettings.setMargin(false);
		
		I18nTextField retrievalName = new I18nTextField(msg);
		retrievalName.setCaption(msg.getMessage("SMSAuthenticatorEditor.formName"));
		configBinder.forField(retrievalName).bind("retrievalName");
		
		LogoFileField logo = new LogoFileField(msg, fileStorageService);
		logo.setCaption(msg.getMessage("SMSAuthenticatorEditor.logo"));
		logo.configureBinding(configBinder, "logo");
		
		interactiveLoginSettings.addComponents(retrievalName, logo);
		CollapsibleLayout wrapper = new CollapsibleLayout(
				msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"),
				interactiveLoginSettings);
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

		try
		{
			return configBinder.getBean().toProperties(fileStorageService, getName());
		} catch (ConfigurationException e)
		{
			throw new FormValidationException("Invalid configuration of the sms verificator", e);
		}
	}

	public static class SMSConfiguration
	{
		private I18nString retrievalName;
		private LocalOrRemoteResource logo;

		public SMSConfiguration(FileStorageService fileStorageService)
		{
			try
			{
				setLogo(new LocalOrRemoteResource(fileStorageService
						.readImageURI(URIHelper.parseURI(Images.mobile_sms.getPath()),
								UI.getCurrent().getTheme())
						.getContents(), Images.mobile_sms.getPath()));
			} catch (Exception e)
			{
				// ok
			}
		}

		public I18nString getRetrievalName()
		{
			return retrievalName;
		}

		public void setRetrievalName(I18nString retrievalName)
		{
			this.retrievalName = retrievalName;
		}

		public String toProperties(FileStorageService fileStorageService, String authName)
		{
			Properties raw = new Properties();

			if (getRetrievalName() != null)
			{
				getRetrievalName().toProperties(raw,
						SMSRetrievalProperties.P + SMSRetrievalProperties.NAME + ".");
			}

			if (getLogo() != null)
			{
				FileFieldUtils.saveInProperties(getLogo(), SMSRetrievalProperties.P + SMSRetrievalProperties.LOGO_URL, raw, fileStorageService,
						StandardOwner.Authenticator.toString(), authName);
			}

			SMSRetrievalProperties prop = new SMSRetrievalProperties(raw);
			return prop.getAsString();
		}

		public void fromProperties(String properties, UnityMessageSource msg, FileStorageService fileStorageService)
		{
			Properties raw = new Properties();
			try
			{
				raw.load(new StringReader(properties));
			} catch (IOException e)
			{
				throw new InternalException("Invalid configuration of the sms verificator", e);
			}

			SMSRetrievalProperties smsRetrievalProperties = new SMSRetrievalProperties(raw);
			setRetrievalName(smsRetrievalProperties.getLocalizedString(msg, SMSRetrievalProperties.NAME));
			if (smsRetrievalProperties.isSet(SMSRetrievalProperties.LOGO_URL))
			{
				String logoUri = smsRetrievalProperties.getValue(SMSRetrievalProperties.LOGO_URL);
				if (!logoUri.isEmpty())
				{
					setLogo(FileFieldUtils.getLogoResourceFromUri(logoUri, fileStorageService));
				}
			}
			
		}

		public LocalOrRemoteResource getLogo()
		{
			return logo;
		}

		public void setLogo(LocalOrRemoteResource logo)
		{
			this.logo = logo;
		}

	}
}
