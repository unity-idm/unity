/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.pam.web;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import com.vaadin.data.Binder;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.pam.PAMProperties;
import pl.edu.icm.unity.pam.PAMVerificator;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.BaseAuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.extensions.PasswordRetrievalProperties;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * PAM authenticator editor
 * 
 * @author P.Piernik
 *
 */
class PamAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private List<String> registrationForms;
	private InputTranslationProfileFieldFactory profileFieldFactory;

	private Binder<PamConfiguration> configBinder;

	PamAuthenticatorEditor(MessageSource msg, List<String> registrationForms,
			InputTranslationProfileFieldFactory profileFieldFactory)

	{
		super(msg);
		this.registrationForms = registrationForms;
		this.profileFieldFactory = profileFieldFactory;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("PamAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(PamConfiguration.class);

		FormLayout header = buildHeaderSection();
		CollapsibleLayout interactiveLoginSettings = buildInteractiveLoginSettingsSection();
		CollapsibleLayout remoteDataMapping = profileFieldFactory.getWrappedFieldInstance(subViewSwitcher,
				configBinder, "translationProfile");

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);
		mainView.addComponent(remoteDataMapping);
		mainView.addComponent(interactiveLoginSettings);

		PamConfiguration config = new PamConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
		}

		configBinder.setBean(config);

		return mainView;
	}

	private FormLayoutWithFixedCaptionWidth buildHeaderSection()
	{
		TextField pamFacility = new TextField(msg.getMessage("PamAuthenticatorEditor.pamFacility"));
		configBinder.forField(pamFacility).asRequired(msg.getMessage("fieldRequired")).bind("pamFacility");

		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);
		header.addComponent(name);
		header.addComponent(pamFacility);
		return header;
	}

	private CollapsibleLayout buildInteractiveLoginSettingsSection()
	{
		FormLayoutWithFixedCaptionWidth interactiveLoginSettings = new FormLayoutWithFixedCaptionWidth();
		interactiveLoginSettings.setMargin(false);

		I18nTextField retrievalName = new I18nTextField(msg);
		retrievalName.setCaption(msg.getMessage("PamAuthenticatorEditor.displayedName"));
		configBinder.forField(retrievalName).bind("retrievalName");
		interactiveLoginSettings.addComponent(retrievalName);

		CheckBox accountAssociation = new CheckBox(msg.getMessage("PamAuthenticatorEditor.accountAssociation"));
		configBinder.forField(accountAssociation).bind("accountAssociation");
		interactiveLoginSettings.addComponent(accountAssociation);

		ComboBox<String> registrationForm = new ComboBox<>(
				msg.getMessage("PamAuthenticatorEditor.registrationForm"));
		registrationForm.setItems(registrationForms);
		configBinder.forField(registrationForm).bind("registrationForm");
		interactiveLoginSettings.addComponent(registrationForm);

		return new CollapsibleLayout(msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"),
				interactiveLoginSettings);
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), PAMVerificator.NAME, getConfiguration(), null);
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
			throw new FormValidationException("Invalid configuration of the pam verificator", e);
		}
	}

	public class PamConfiguration
	{
		private I18nString retrievalName;
		private String pamFacility;
		private TranslationProfile translationProfile;
		private boolean accountAssociation;
		private String registrationForm;

		public PamConfiguration()
		{
			translationProfile = TranslationProfileGenerator.generateEmbeddedEmptyInputProfile();
		}

		public String toProperties(MessageSource msg)
		{
			Properties raw = new Properties();

			raw.put(PAMProperties.PREFIX + PAMProperties.PAM_FACILITY, pamFacility);
			raw.put(PAMProperties.PREFIX + CommonWebAuthnProperties.TRANSLATION_PROFILE,
					translationProfile.getName());

			if (getRetrievalName() != null)
			{
				getRetrievalName().toProperties(raw,
						PasswordRetrievalProperties.P + PasswordRetrievalProperties.NAME, msg);
			}

			raw.put(PasswordRetrievalProperties.P + PasswordRetrievalProperties.ENABLE_ASSOCIATION,
					String.valueOf(isAccountAssociation()));
			if (getRegistrationForm() != null)
			{
				raw.put(PasswordRetrievalProperties.P
						+ PasswordRetrievalProperties.REGISTRATION_FORM_FOR_UNKNOWN,
						getRegistrationForm());
			}

			try
			{
				raw.put(PAMProperties.PREFIX + CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE,
						Constants.MAPPER.writeValueAsString(
								getTranslationProfile().toJsonObject()));
			} catch (Exception e)
			{
				throw new InternalException("Can't serialize authenticator translation profile to JSON",
						e);
			}

			PAMProperties prop = new PAMProperties(raw);
			return prop.getAsString();
		}

		public void fromProperties(String source, MessageSource msg)
		{
			Properties raw = new Properties();
			try
			{
				raw.load(new StringReader(source));
			} catch (IOException e)
			{
				throw new InternalException("Invalid configuration of the pam verificator", e);
			}

			PAMProperties pamProp = new PAMProperties(raw);
			setPamFacility(pamProp.getValue(PAMProperties.PAM_FACILITY));

			if (pamProp.isSet(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE))
			{
				setTranslationProfile(TranslationProfileGenerator.getProfileFromString(pamProp
						.getValue(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE)));

			} else
			{
				setTranslationProfile(TranslationProfileGenerator.generateIncludeInputProfile(
						pamProp.getValue(CommonWebAuthnProperties.TRANSLATION_PROFILE)));
			}

			PasswordRetrievalProperties passwordRetrievalProperties = new PasswordRetrievalProperties(raw);
			setRetrievalName(passwordRetrievalProperties.getLocalizedStringWithoutFallbackToDefault(msg,
					PasswordRetrievalProperties.NAME));
			setAccountAssociation(passwordRetrievalProperties
					.getBooleanValue(PasswordRetrievalProperties.ENABLE_ASSOCIATION));
			setRegistrationForm(passwordRetrievalProperties
					.getValue(PasswordRetrievalProperties.REGISTRATION_FORM_FOR_UNKNOWN));
		}

		public I18nString getRetrievalName()
		{
			return retrievalName;
		}

		public void setRetrievalName(I18nString retrivalName)
		{
			this.retrievalName = retrivalName;
		}

		public String getPamFacility()
		{
			return pamFacility;
		}

		public void setPamFacility(String pamFacility)
		{
			this.pamFacility = pamFacility;
		}

		public TranslationProfile getTranslationProfile()
		{
			return translationProfile;
		}

		public void setTranslationProfile(TranslationProfile translationProfile)
		{
			this.translationProfile = translationProfile;
		}

		public boolean isAccountAssociation()
		{
			return accountAssociation;
		}

		public void setAccountAssociation(boolean accountAssociation)
		{
			this.accountAssociation = accountAssociation;
		}

		public String getRegistrationForm()
		{
			return registrationForm;
		}

		public void setRegistrationForm(String registrationForm)
		{
			this.registrationForm = registrationForm;
		}

	}

}
