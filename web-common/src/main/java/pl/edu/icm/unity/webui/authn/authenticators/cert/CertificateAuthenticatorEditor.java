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
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.FileStorageService.StandardOwner;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
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
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.file.FileFieldUtils;
import pl.edu.icm.unity.webui.common.file.ImageField;
import pl.edu.icm.unity.webui.common.file.ImageUtils;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * Certificate authenticator editor
 * 
 * @author P.Piernik
 *
 */
class CertificateAuthenticatorEditor extends BaseLocalAuthenticatorEditor implements AuthenticatorEditor
{
	private UnityMessageSource msg;
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private Binder<CertConfiguration> configBinder;

	CertificateAuthenticatorEditor(UnityMessageSource msg, FileStorageService fileStorageService, URIAccessService uriAccessService,
			Collection<CredentialDefinition> credentialDefinitions) throws EngineException
	{
		super(msg, credentialDefinitions.stream().filter(c -> c.getTypeId().equals(CertificateVerificator.NAME))
				.map(c -> c.getName()).collect(Collectors.toList()));
		this.msg = msg;
		this.uriAccessService = uriAccessService;
		this.fileStorageService = fileStorageService;
		
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

		CertConfiguration config = new CertConfiguration(uriAccessService);
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg, uriAccessService);
		}
		configBinder.setBean(config);

		return main;
	}

	private CollapsibleLayout buildInteractiveLoginSettingsSection()
	{
		FormLayoutWithFixedCaptionWidth interactiveLoginSettings = new FormLayoutWithFixedCaptionWidth();
		interactiveLoginSettings.setMargin(false);

		I18nTextField retrievalName = new I18nTextField(msg);
		retrievalName.setCaption(msg.getMessage("CertificateAuthenticatorEditor.displayedName"));
		configBinder.forField(retrievalName).bind("retrievalName");

		ImageField logo = new ImageField(msg, uriAccessService);
		logo.setCaption(msg.getMessage("CertificateAuthenticatorEditor.logo"));
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
		return new AuthenticatorDefinition(getName(), CertificateVerificator.NAME, getConfiguration(),
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
			throw new FormValidationException("Invalid configuration of the certificate verificator", e);
		}
	}

	public static class CertConfiguration
	{
		private I18nString retrievalName;
		private LocalOrRemoteResource logo;

		public CertConfiguration(URIAccessService uriAccessService)
		{
			setLogo(ImageUtils.getImageFromUriSave(Images.certificate.getPath(), uriAccessService));
		}

		public I18nString getRetrievalName()
		{
			return retrievalName;
		}

		public void setRetrievalName(I18nString retrievalName)
		{
			this.retrievalName = retrievalName;
		}

		public LocalOrRemoteResource getLogo()
		{
			return logo;
		}

		public void setLogo(LocalOrRemoteResource logo)
		{
			this.logo = logo;
		}

		public String toProperties(FileStorageService fileStorageService, String authName)
		{
			Properties raw = new Properties();

			if (getRetrievalName() != null)
			{
				getRetrievalName().toProperties(raw,
						TLSRetrievalProperties.P + TLSRetrievalProperties.NAME);
			}

			if (getLogo() != null)
			{
				FileFieldUtils.saveInProperties(getLogo(),
						TLSRetrievalProperties.P + TLSRetrievalProperties.LOGO_URL, raw,
						fileStorageService, StandardOwner.AUTHENTICATOR.toString(), authName);
			}

			TLSRetrievalProperties prop = new TLSRetrievalProperties(raw);
			return prop.getAsString();
		}

		public void fromProperties(String properties, UnityMessageSource msg,
				URIAccessService uriAccessService)
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
			setRetrievalName(certRetrievalProperties.getLocalizedString(msg, TLSRetrievalProperties.NAME));
			if (certRetrievalProperties.isSet(TLSRetrievalProperties.LOGO_URL))
			{
				setLogo(ImageUtils.getImageFromUriSave(certRetrievalProperties.getValue(TLSRetrievalProperties.LOGO_URL),
							uriAccessService));
			}	
			
		}
	}
}
