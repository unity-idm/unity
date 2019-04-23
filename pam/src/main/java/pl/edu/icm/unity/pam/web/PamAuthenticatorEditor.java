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
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.pam.PAMProperties;
import pl.edu.icm.unity.pam.PAMVerificator;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.types.translation.TranslationProfile;
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
public class PamAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private List<String> registrationForms;
	private InputTranslationProfileFieldFactory profileFieldFactory;

	private Binder<PamConfiguration> configBinder;

	public PamAuthenticatorEditor(UnityMessageSource msg, List<String> registrationForms,
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

		I18nTextField retrivalName = new I18nTextField(msg);
		retrivalName.setCaption(msg.getMessage("PamAuthenticatorEditor.passwordName"));
		interactiveLoginSettings.addComponent(retrivalName);

		CheckBox accountAssociation = new CheckBox(msg.getMessage("PamAuthenticatorEditor.accountAssociation"));
		interactiveLoginSettings.addComponent(accountAssociation);

		ComboBox<String> registrationForm = new ComboBox<>(
				msg.getMessage("PamAuthenticatorEditor.registrationForm"));
		registrationForm.setItems(registrationForms);
		interactiveLoginSettings.addComponent(registrationForm);

		configBinder.forField(retrivalName).bind("retrivalName");
		configBinder.forField(accountAssociation).bind("accountAssociation");
		configBinder.forField(registrationForm).bind("registrationForm");

		return new CollapsibleLayout(msg.getMessage("PamAuthenticatorEditor.interactiveLoginSettings"),
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
			return configBinder.getBean().toProperties();
		} catch (ConfigurationException e)
		{
			throw new FormValidationException("Invalid configuration of the pam verificator", e);
		}
	}

	public class PamConfiguration
	{
		private I18nString retrivalName;
		private String pamFacility;
		private TranslationProfile translationProfile;
		private boolean accountAssociation;
		private String registrationForm;

		public PamConfiguration()
		{
			translationProfile = TranslationProfileGenerator.generateEmptyInputProfile();
		}

		public I18nString getRetrivalName()
		{
			return retrivalName;
		}

		public void setRetrivalName(I18nString retrivalName)
		{
			this.retrivalName = retrivalName;
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

		public String toProperties()
		{
			Properties raw = new Properties();

			raw.put(PAMProperties.PREFIX + PAMProperties.PAM_FACILITY, pamFacility);
			raw.put(PAMProperties.PREFIX + CommonWebAuthnProperties.TRANSLATION_PROFILE,
					translationProfile.getName());

			if (retrivalName != null)
			{
				retrivalName.toProperties(raw,
						PasswordRetrievalProperties.P + PasswordRetrievalProperties.NAME + ".");
			}

			raw.put(PasswordRetrievalProperties.P + PasswordRetrievalProperties.ENABLE_ASSOCIATION,
					String.valueOf(accountAssociation));
			if (registrationForm != null)
			{
				raw.put(PasswordRetrievalProperties.P
						+ PasswordRetrievalProperties.REGISTRATION_FORM_FOR_UNKNOWN,
						registrationForm);
			}

			try
			{
				raw.put(PAMProperties.PREFIX + CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE,
						Constants.MAPPER.writeValueAsString(translationProfile.toJsonObject()));
			} catch (Exception e)
			{
				throw new InternalException("Can't serialize authenticator translation profile to JSON",
						e);
			}

			PAMProperties prop = new PAMProperties(raw);
			return prop.getAsString();
		}

		public void fromProperties(String source, UnityMessageSource msg)
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
			pamFacility = pamProp.getValue(PAMProperties.PAM_FACILITY);

			if (pamProp.isSet(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE))
			{
				translationProfile = TranslationProfileGenerator.getProfileFromString(pamProp
						.getValue(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE));

			} else
			{
				translationProfile = TranslationProfileGenerator.generateIncludeInputProfile(
						pamProp.getValue(CommonWebAuthnProperties.TRANSLATION_PROFILE));
			}

			PasswordRetrievalProperties passwordRetrievalProperties = new PasswordRetrievalProperties(raw);
			retrivalName = passwordRetrievalProperties.getLocalizedString(msg,
					PasswordRetrievalProperties.NAME);
			accountAssociation = passwordRetrievalProperties
					.getBooleanValue(PasswordRetrievalProperties.ENABLE_ASSOCIATION);
			registrationForm = passwordRetrievalProperties
					.getValue(PasswordRetrievalProperties.REGISTRATION_FORM_FOR_UNKNOWN);
		}

	}

}
