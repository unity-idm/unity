/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.sp.console;

import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import io.imunity.console.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
import io.imunity.vaadin.elements.NoSpaceValidator;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.api.UnitySubView;
import pl.edu.icm.unity.base.message.MessageSource;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.EDIT_VIEW_ACTION_BUTTONS_LAYOUT;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;


class EditTrustedFederationSubView extends VerticalLayout implements UnitySubView
{
	private final MessageSource msg;
	private final Binder<SAMLAuthnTrustedFederationConfiguration> binder;
	private final boolean editMode;
	private final Set<String> validators;
	private final Set<String> certificates;
	private final Set<String> registrationForms;
	private final Set<String> usedNames;

	EditTrustedFederationSubView(MessageSource msg,
			InputTranslationProfileFieldFactory profileFieldFactory, SAMLAuthnTrustedFederationConfiguration toEdit,
			SubViewSwitcher subViewSwitcher, Set<String> usedNames, Set<String> validators,
			Set<String> certificates, Set<String> registrationForms,
			Consumer<SAMLAuthnTrustedFederationConfiguration> onConfirm, Runnable onCancel, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.validators = validators;
		this.certificates = certificates;
		this.registrationForms = registrationForms;
		this.usedNames = usedNames;

		editMode = toEdit != null;

		binder = new Binder<>(SAMLAuthnTrustedFederationConfiguration.class);
		FormLayout header = buildHeaderSection();
		AccordionPanel remoteDataMapping = profileFieldFactory.getWrappedFieldInstance(subViewSwitcher,
				binder, "translationProfile");

		binder.setBean(editMode ? toEdit.clone() : new SAMLAuthnTrustedFederationConfiguration());

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.add(header);
		mainView.add(remoteDataMapping);

		Button cancelButton = new Button(msg.getMessage("cancel"), event -> onCancel.run());
		cancelButton.setWidthFull();
		Button updateButton = new Button(editMode ? msg.getMessage("update") :  msg.getMessage("create"), event ->
		{
			try
			{
				onConfirm.accept(getTrustedFederation());
			} catch (FormValidationException e)
			{
				notificationPresenter.showError(msg.getMessage(
						"EditTrustedFederationSubView.invalidConfiguration"), e.getMessage());
			}
		});
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		updateButton.setWidthFull();
		HorizontalLayout buttonsLayout = new HorizontalLayout(cancelButton, updateButton);
		buttonsLayout.setClassName(EDIT_VIEW_ACTION_BUTTONS_LAYOUT.getName());
		mainView.add(buttonsLayout);

		add(mainView);
	}

	private FormLayout buildHeaderSection()
	{
		FormLayout header = new FormLayout();
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		TextField name = new TextField();
		binder.forField(name)
				.asRequired(msg.getMessage("fieldRequired"))
				.withValidator(new NoSpaceValidator(msg::getMessage))
				.withValidator((s, c) -> {
					if (usedNames.contains(s))
					{
						return ValidationResult.error(msg
								.getMessage("EditTrustedFederationSubView.nameExists"));
					} else
					{
						return ValidationResult.ok();
					}

				}).bind(SAMLAuthnTrustedFederationConfiguration::getName, SAMLAuthnTrustedFederationConfiguration::setName);
		header.addFormItem(name, msg.getMessage("EditTrustedFederationSubView.name"));
		name.focus();

		TextField url = new TextField();
		url.setWidth(TEXT_FIELD_BIG.value());
		binder.forField(url).asRequired(msg.getMessage("fieldRequired"))
				.bind(SAMLAuthnTrustedFederationConfiguration::getUrl, SAMLAuthnTrustedFederationConfiguration::setUrl);
		header.addFormItem(url, msg.getMessage("EditTrustedFederationSubView.url"));
		
		MultiSelectComboBox<String> excludedIdps = new CustomValuesMultiSelectComboBox();
		excludedIdps.setWidthFull();
		excludedIdps.setPlaceholder(msg.getMessage("typeAndConfirm"));
		binder.forField(excludedIdps)
				.withConverter(List::copyOf, HashSet::new)
				.bind(SAMLAuthnTrustedFederationConfiguration::getExcludedIdps, SAMLAuthnTrustedFederationConfiguration::setExcludedIdps);
		header.addFormItem(excludedIdps, msg.getMessage("EditTrustedFederationSubView.excludedIdps"));

		Select<String> httpsTruststore = new Select<>();
		httpsTruststore.setItems(validators);
		httpsTruststore.setEmptySelectionAllowed(true);
		binder.forField(httpsTruststore)
				.bind(SAMLAuthnTrustedFederationConfiguration::getHttpsTruststore, SAMLAuthnTrustedFederationConfiguration::setHttpsTruststore);
		header.addFormItem(httpsTruststore, msg.getMessage("EditTrustedFederationSubView.httpsTruststore"));

		Checkbox ignoreSignatureVerification = new Checkbox(
				msg.getMessage("EditTrustedFederationSubView.ignoreSignatureVerification"));
		binder.forField(ignoreSignatureVerification)
				.bind(SAMLAuthnTrustedFederationConfiguration::isIgnoreSignatureVerification, SAMLAuthnTrustedFederationConfiguration::setIgnoreSignatureVerification);
		header.addFormItem(ignoreSignatureVerification, "");

		Select<String> signatureVerificationCertificate = new Select<>();
		signatureVerificationCertificate.setItems(certificates);
		binder.forField(signatureVerificationCertificate).asRequired(
				(v, c) -> ((v == null || v.isEmpty()) && !ignoreSignatureVerification.getValue())
						? ValidationResult.error(msg.getMessage("fieldRequired"))
						: ValidationResult.ok())
				.bind(SAMLAuthnTrustedFederationConfiguration::getSignatureVerificationCertificate, SAMLAuthnTrustedFederationConfiguration::setSignatureVerificationCertificate);
		header.addFormItem(signatureVerificationCertificate, msg.getMessage("EditTrustedFederationSubView.signatureVerificationCertificate"));
		ignoreSignatureVerification.addValueChangeListener(event ->
		{
			signatureVerificationCertificate.setRequiredIndicatorVisible(!event.getValue());
			signatureVerificationCertificate.setEmptySelectionAllowed(event.getValue());
		});

		IntegerField refreshInterval = new IntegerField();
		refreshInterval.setMin(0);
		binder.forField(refreshInterval).asRequired(msg.getMessage("fieldRequired"))
				.bind(SAMLAuthnTrustedFederationConfiguration::getRefreshInterval, SAMLAuthnTrustedFederationConfiguration::setRefreshInterval);
		header.addFormItem(refreshInterval, msg.getMessage("EditTrustedFederationSubView.refreshInterval"));

		Select<String> registrationForm = new Select<>();
		registrationForm.setItems(registrationForms);
		registrationForm.setEmptySelectionAllowed(true);
		binder.forField(registrationForm)
				.bind(SAMLAuthnTrustedFederationConfiguration::getRegistrationForm, SAMLAuthnTrustedFederationConfiguration::setRegistrationForm);
		header.addFormItem(registrationForm, msg.getMessage("EditTrustedFederationSubView.registrationForm"));

		return header;
	}

	@Override
	public List<String> getBreadcrumbs()
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
