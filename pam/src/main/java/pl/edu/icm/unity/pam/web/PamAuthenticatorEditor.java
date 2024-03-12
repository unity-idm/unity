/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.pam.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.console.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.vaadin.auth.CommonWebAuthnProperties;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.BaseAuthenticatorEditor;
import io.imunity.vaadin.auth.extensions.PasswordRetrievalProperties;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.pam.PAMProperties;
import pl.edu.icm.unity.pam.PAMVerificator;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;


class PamAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private final List<String> registrationForms;
	private final InputTranslationProfileFieldFactory profileFieldFactory;

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
		AccordionPanel interactiveLoginSettingsSection = buildInteractiveLoginSettingsSection();
		interactiveLoginSettingsSection.setWidthFull();
		AccordionPanel remoteDataMapping = profileFieldFactory.getWrappedFieldInstance(subViewSwitcher,
				configBinder, "translationProfile");
		remoteDataMapping.setWidthFull();

		VerticalLayout mainView = new VerticalLayout();
		mainView.setPadding(false);
		mainView.add(header, remoteDataMapping, interactiveLoginSettingsSection);

		PamConfiguration config = new PamConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
		}

		configBinder.setBean(config);

		return mainView;
	}

	private FormLayout buildHeaderSection()
	{
		TextField pamFacility = new TextField();
		configBinder.forField(pamFacility).asRequired(msg.getMessage("fieldRequired")).bind("pamFacility");

		FormLayout header = new FormLayout();
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));
		header.addFormItem(pamFacility, msg.getMessage("PamAuthenticatorEditor.pamFacility"));
		return header;
	}

	private AccordionPanel buildInteractiveLoginSettingsSection()
	{
		FormLayout interactiveLoginSettings = new FormLayout();
		interactiveLoginSettings.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		interactiveLoginSettings.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		LocalizedTextFieldDetails retrievalName = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(),
				msg.getLocale());
		retrievalName.setWidth(TEXT_FIELD_MEDIUM.value());
		configBinder.forField(retrievalName)
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.bind(PamConfiguration::getRetrievalName, PamConfiguration::setRetrievalName);
		interactiveLoginSettings.addFormItem(retrievalName, msg.getMessage("PamAuthenticatorEditor.displayedName"));

		Checkbox accountAssociation = new Checkbox(msg.getMessage("PamAuthenticatorEditor.accountAssociation"));
		configBinder.forField(accountAssociation)
				.bind(PamConfiguration::isAccountAssociation, PamConfiguration::setAccountAssociation);
		interactiveLoginSettings.addFormItem(accountAssociation, "");

		Select<String> registrationForm = new Select<>();
		registrationForm.setWidth(TEXT_FIELD_MEDIUM.value());
		registrationForm.setItems(registrationForms);
		registrationForm.setEmptySelectionAllowed(true);
		configBinder.forField(registrationForm)
				.bind(PamConfiguration::getRegistrationForm, PamConfiguration::setRegistrationForm);
		interactiveLoginSettings.addFormItem(registrationForm,
				msg.getMessage("PamAuthenticatorEditor.registrationForm"));

		return new AccordionPanel(msg.getMessage("BaseAuthenticatorEditor.interactiveLoginSettings"),
				interactiveLoginSettings);
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
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
		private I18nString retrievalName = new I18nString();
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
