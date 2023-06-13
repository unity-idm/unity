/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.sp.console;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.common.validators.NoSpaceValidator;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

/**
 * View for edit SAML trusted federation
 * 
 * @author P.Piernik
 *
 */
class EditTrustedFederationSubView extends CustomComponent implements UnitySubView
{
	private MessageSource msg;
	private Binder<SAMLAuthnTrustedFederationConfiguration> binder;
	private boolean editMode = false;
	private Set<String> validators;
	private Set<String> certificates;
	private Set<String> registrationForms;
	private Set<String> usedNames;

	EditTrustedFederationSubView(MessageSource msg,
			InputTranslationProfileFieldFactory profileFieldFactory, SAMLAuthnTrustedFederationConfiguration toEdit,
			SubViewSwitcher subViewSwitcher, Set<String> usedNames, Set<String> validators,
			Set<String> certificates, Set<String> registrationForms,
			Consumer<SAMLAuthnTrustedFederationConfiguration> onConfirm, Runnable onCancel)
	{
		this.msg = msg;
		this.validators = validators;
		this.certificates = certificates;
		this.registrationForms = registrationForms;
		this.usedNames = usedNames;

		editMode = toEdit != null;

		binder = new Binder<>(SAMLAuthnTrustedFederationConfiguration.class);
		FormLayout header = buildHeaderSection();
		CollapsibleLayout remoteDataMapping = profileFieldFactory.getWrappedFieldInstance(subViewSwitcher,
				binder, "translationProfile");

		binder.setBean(editMode ? toEdit.clone() : new SAMLAuthnTrustedFederationConfiguration());

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);
		mainView.addComponent(remoteDataMapping);

		Runnable onConfirmR = () -> {
			try
			{
				onConfirm.accept(getTrustedFederation());
			} catch (FormValidationException e)
			{
				NotificationPopup.showError(msg, msg.getMessage(
						"EditTrustedFederationSubView.invalidConfiguration"), e);
			}
		};

		mainView.addComponent(editMode
				? StandardButtonsHelper.buildConfirmEditButtonsBar(msg, onConfirmR, onCancel)
				: StandardButtonsHelper.buildConfirmNewButtonsBar(msg, onConfirmR, onCancel));

		setCompositionRoot(mainView);

	}

	private FormLayout buildHeaderSection()
	{
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);

		TextField name = new TextField(msg.getMessage("EditTrustedFederationSubView.name"));
		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).withValidator(new NoSpaceValidator(msg))
				.withValidator((s, c) -> {
					if (usedNames.contains(s))
					{
						return ValidationResult.error(msg
								.getMessage("EditTrustedFederationSubView.nameExists"));
					} else
					{
						return ValidationResult.ok();
					}

				}).bind("name");
		header.addComponent(name);
		name.focus();

		TextField url = new TextField(msg.getMessage("EditTrustedFederationSubView.url"));
		url.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		binder.forField(url).asRequired(msg.getMessage("fieldRequired")).bind("url");
		header.addComponent(url);
		
		ChipsWithTextfield excludedIdps = new ChipsWithTextfield(msg);
		excludedIdps.setCaption(msg.getMessage("EditTrustedFederationSubView.excludedIdps"));
		url.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		binder.forField(excludedIdps).bind("excludedIdps");
		header.addComponent(excludedIdps);

		ComboBox<String> httpsTruststore = new ComboBox<>(
				msg.getMessage("EditTrustedFederationSubView.httpsTruststore"));
		httpsTruststore.setItems(validators);
		binder.forField(httpsTruststore).bind("httpsTruststore");
		header.addComponent(httpsTruststore);

		CheckBox ignoreSignatureVerification = new CheckBox(
				msg.getMessage("EditTrustedFederationSubView.ignoreSignatureVerification"));
		binder.forField(ignoreSignatureVerification).bind("ignoreSignatureVerification");
		header.addComponent(ignoreSignatureVerification);

		ComboBox<String> signatureVerificationCertificate = new ComboBox<>(
				msg.getMessage("EditTrustedFederationSubView.signatureVerificationCertificate"));
		signatureVerificationCertificate.setItems(certificates);
		binder.forField(signatureVerificationCertificate).asRequired(
				(v, c) -> ((v == null || v.isEmpty()) && !ignoreSignatureVerification.getValue())
						? ValidationResult.error(msg.getMessage("fieldRequired"))
						: ValidationResult.ok())
				.bind("signatureVerificationCertificate");
		header.addComponent(signatureVerificationCertificate);

		TextField refreshInterval = new TextField();
		refreshInterval.setCaption(msg.getMessage("EditTrustedFederationSubView.refreshInterval"));
		binder.forField(refreshInterval).asRequired(msg.getMessage("fieldRequired"))
				.withConverter(new StringToIntegerConverter(msg.getMessage("notAPositiveNumber")))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 0, null))
				.bind("refreshInterval");
		header.addComponent(refreshInterval);

		ComboBox<String> registrationForm = new ComboBox<>(
				msg.getMessage("EditTrustedFederationSubView.registrationForm"));
		registrationForm.setItems(registrationForms);
		binder.forField(registrationForm).bind("registrationForm");
		header.addComponent(registrationForm);

		return header;
	}

	@Override
	public List<String> getBredcrumbs()
	{	
		if (editMode)
			return Arrays.asList(msg.getMessage("EditTrustedFederationSubView.trustedFederation"),
					binder.getBean().getName());
		else
			return Arrays.asList(msg.getMessage("EditTrustedFederationSubView.newTrustedFederation"));
	
	}

	private SAMLAuthnTrustedFederationConfiguration getTrustedFederation() throws FormValidationException
	{
		if (binder.validate().hasErrors())
			throw new FormValidationException();

		return binder.getBean();
	}

}
