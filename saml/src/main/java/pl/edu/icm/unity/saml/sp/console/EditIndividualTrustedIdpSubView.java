/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.sp.console;

import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import io.imunity.console_utils.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.vaadin.auth.binding.ToggleWithDefault;
import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.elements.NoSpaceValidator;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.api.UnitySubView;
import io.imunity.vaadin.endpoint.common.file.FileField;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.*;
import java.util.function.Consumer;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;


class EditIndividualTrustedIdpSubView extends VerticalLayout implements UnitySubView
{
	private final MessageSource msg;
	private final Binder<SAMLIndividualTrustedSamlIdpConfiguration> configBinder;
	private final Set<String> certificates;
	private final Set<String> registrationForms;
	private final Set<String> usedNames;
	private final UnityServerConfiguration serverConfig;
	private final boolean editMode;

	EditIndividualTrustedIdpSubView(MessageSource msg, NotificationPresenter notificationPresenter,
			InputTranslationProfileFieldFactory profileFieldFactory,
			SAMLIndividualTrustedSamlIdpConfiguration toEdit, SubViewSwitcher subViewSwitcher,
			Set<String> usedNames, Set<String> certificates, Set<String> registrationForms,
			Consumer<SAMLIndividualTrustedSamlIdpConfiguration> onConfirm, Runnable onCancel,
			UnityServerConfiguration serverConfig)
	{
		this.msg = msg;
		this.certificates = certificates;
		this.registrationForms = registrationForms;
		this.usedNames = usedNames;
		this.serverConfig = serverConfig;
		editMode = toEdit != null;

		configBinder = new Binder<>(SAMLIndividualTrustedSamlIdpConfiguration.class);

		FormLayout header = buildHeaderSection();
		AccordionPanel singleLogout = buildSingleLogoutSection();
		singleLogout.setWidthFull();
		AccordionPanel remoteDataMapping = profileFieldFactory.getWrappedFieldInstance(subViewSwitcher,
				configBinder, "translationProfile");
		remoteDataMapping.setWidthFull();


		configBinder.setBean(editMode ? toEdit.clone() : new SAMLIndividualTrustedSamlIdpConfiguration());

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.add(header);
		mainView.add(singleLogout, remoteDataMapping);

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
						"EditIndividualTrustedIdpSubView.invalidConfiguration"), e.getMessage());
			}
		});
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		updateButton.setWidthFull();
		HorizontalLayout buttonsLayout = new HorizontalLayout(cancelButton, updateButton);
		buttonsLayout.setClassName("u-edit-view-action-buttons-layout");
		mainView.add(buttonsLayout);

		add(mainView);
	}

	private FormLayout buildHeaderSection()
	{
		FormLayout header = new FormLayout();
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		TextField name = new TextField();
		configBinder.forField(name).asRequired(msg.getMessage("fieldRequired"))
				.withValidator(new NoSpaceValidator(msg::getMessage)).withValidator((s, c) -> {
					if (usedNames.contains(s))
					{
						return ValidationResult.error(msg.getMessage(
								"EditIndividualTrustedIdpSubView.nameExists"));
					} else
					{
						return ValidationResult.ok();
					}

				}).bind(SAMLIndividualTrustedSamlIdpConfiguration::getName, SAMLIndividualTrustedSamlIdpConfiguration::setName);
		header.addFormItem(name, msg.getMessage("EditIndividualTrustedIdpSubView.name"));

		LocalizedTextFieldDetails displayedName = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		displayedName.setWidth(TEXT_FIELD_MEDIUM.value());
		configBinder.forField(displayedName)
				.asRequired(msg.getMessage("fieldRequired"))
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.withValidator(value -> !value.getMap().isEmpty(), msg.getMessage("fieldRequired"))
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getDisplayedName, SAMLIndividualTrustedSamlIdpConfiguration::setDisplayedName);
		header.addFormItem(displayedName, msg.getMessage("EditIndividualTrustedIdpSubView.displayedName"));

		FileField logo = new FileField(msg, "image/*", "logo.jpg", serverConfig.getFileSizeLimit());
		configBinder.forField(logo)
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getLogo, SAMLIndividualTrustedSamlIdpConfiguration::setLogo);
		header.addFormItem(logo, msg.getMessage("EditIndividualTrustedIdpSubView.logo"));
		
		TextField id = new TextField();
		id.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(id)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getId, SAMLIndividualTrustedSamlIdpConfiguration::setId);
		header.addFormItem(id, msg.getMessage("EditIndividualTrustedIdpSubView.id"));

		TextField address = new TextField();
		address.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(address)
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getAddress, SAMLIndividualTrustedSamlIdpConfiguration::setAddress);
		header.addFormItem(address, msg.getMessage("EditIndividualTrustedIdpSubView.address"));

		MultiSelectComboBox<String> requestedNameFormats = new CustomValuesMultiSelectComboBox();
		requestedNameFormats.setWidth(TEXT_FIELD_BIG.value());
		requestedNameFormats.setItems(SAMLAuthenticatorEditor.STANDART_NAME_FORMATS);
		requestedNameFormats.setPlaceholder(msg.getMessage("typeOrSelect"));
		header.addFormItem(requestedNameFormats, msg.getMessage("EditIndividualTrustedIdpSubView.requestedNameFormats"));
		configBinder.forField(requestedNameFormats)
				.withConverter(List::copyOf, HashSet::new)
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getRequestedNameFormats, SAMLIndividualTrustedSamlIdpConfiguration::setRequestedNameFormats);

		Checkbox signRequest = new Checkbox(msg.getMessage("EditIndividualTrustedIdpSubView.signRequest"));

		Select<Binding> binding = new Select<>();
		binding.setItems(Binding.values());
		binding.setEmptySelectionAllowed(false);
		binding.addValueChangeListener(e -> {
			boolean v = e.getValue().equals(Binding.HTTP_POST);
			signRequest.setEnabled(v);
			if (!v)
			{
				signRequest.setValue(false);
			}

		});

		configBinder.forField(binding)
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getBinding, SAMLIndividualTrustedSamlIdpConfiguration::setBinding);
		configBinder.forField(signRequest).withValidator((v, c) -> {
			if (v != null && v && (binding.getValue().equals(Binding.HTTP_REDIRECT)
					|| binding.getValue().equals(Binding.SOAP)))
			{
				return ValidationResult.error(msg.getMessage(
						"EditIndividualTrustedIdpSubView.signRequestValidationError"));
			}
			return ValidationResult.ok();

		}).bind(SAMLIndividualTrustedSamlIdpConfiguration::isSignRequest, SAMLIndividualTrustedSamlIdpConfiguration::setSignRequest);

		header.addFormItem(binding, msg.getMessage("EditIndividualTrustedIdpSubView.binding"));
		header.addFormItem(signRequest, "");

		MultiSelectComboBox<String> certificatesCombo = new MultiSelectComboBox<>();
		certificatesCombo.setItems(new ArrayList<>(certificates));
		configBinder.forField(certificatesCombo)
				.withConverter(List::copyOf, HashSet::new)
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getCertificates, SAMLIndividualTrustedSamlIdpConfiguration::setCertificates);
		header.addFormItem(certificatesCombo, msg.getMessage("EditIndividualTrustedIdpSubView.certificates"));

		TextField groupAttribute = new TextField();
		configBinder.forField(groupAttribute)
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getGroupMembershipAttribute, SAMLIndividualTrustedSamlIdpConfiguration::setGroupMembershipAttribute);
		header.addFormItem(groupAttribute, msg.getMessage("EditIndividualTrustedIdpSubView.groupMembershipAttribute"));
		
		ComboBox<String> registrationForm = new ComboBox<>();
		registrationForm.setItems(registrationForms);
		header.addFormItem(registrationForm, msg.getMessage("EditIndividualTrustedIdpSubView.registrationForm"));
		configBinder.forField(registrationForm)
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getRegistrationForm, SAMLIndividualTrustedSamlIdpConfiguration::setRegistrationForm);

		Select<ToggleWithDefault> accountAssociation = new Select<>();
		accountAssociation.setItemLabelGenerator(item -> msg.getMessage("EnableDisableCombo." + item));
		accountAssociation.setItems(ToggleWithDefault.values());
		accountAssociation.setValue(ToggleWithDefault.bydefault);
		configBinder.forField(accountAssociation)
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getAccountAssociation, SAMLIndividualTrustedSamlIdpConfiguration::setAccountAssociation);
		header.addFormItem(accountAssociation, msg.getMessage("EditIndividualTrustedIdpSubView.accountAssociation"));

		return header;
	}

	private AccordionPanel buildSingleLogoutSection()
	{
		FormLayout singleLogout = new FormLayout();
		singleLogout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		singleLogout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		TextField postLogoutEndpoint = new TextField();
		postLogoutEndpoint.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(postLogoutEndpoint)
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getPostLogoutEndpoint, SAMLIndividualTrustedSamlIdpConfiguration::setPostLogoutEndpoint);
		singleLogout.addFormItem(postLogoutEndpoint, msg.getMessage("EditIndividualTrustedIdpSubView.postLogoutEndpoint"));

		TextField postLogoutResponseEndpoint = new TextField();
		postLogoutResponseEndpoint.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(postLogoutResponseEndpoint)
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getPostLogoutResponseEndpoint, SAMLIndividualTrustedSamlIdpConfiguration::setPostLogoutEndpoint);
		singleLogout.addFormItem(postLogoutResponseEndpoint, msg.getMessage("EditIndividualTrustedIdpSubView.postLogoutResponseEndpoint"));

		TextField redirectLogoutEndpoint = new TextField();
		redirectLogoutEndpoint.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(redirectLogoutEndpoint)
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getRedirectLogoutEndpoint, SAMLIndividualTrustedSamlIdpConfiguration::setRedirectLogoutEndpoint);
		singleLogout.addFormItem(redirectLogoutEndpoint, msg.getMessage("EditIndividualTrustedIdpSubView.redirectLogoutEndpoint"));

		TextField redirectLogoutResponseEndpoint = new TextField();
		redirectLogoutResponseEndpoint.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(redirectLogoutResponseEndpoint)
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getRedirectLogoutResponseEndpoint, SAMLIndividualTrustedSamlIdpConfiguration::setRedirectLogoutResponseEndpoint);
		singleLogout.addFormItem(redirectLogoutResponseEndpoint, msg.getMessage("EditIndividualTrustedIdpSubView.redirectLogoutResponseEndpoint"));

		TextField soapLogoutEndpoint = new TextField();
		soapLogoutEndpoint.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(soapLogoutEndpoint)
				.bind(SAMLIndividualTrustedSamlIdpConfiguration::getSoapLogoutEndpoint, SAMLIndividualTrustedSamlIdpConfiguration::setSoapLogoutEndpoint);
		singleLogout.addFormItem(soapLogoutEndpoint, msg.getMessage("EditIndividualTrustedIdpSubView.soapLogoutEndpoint"));

		return new AccordionPanel(msg.getMessage("EditIndividualTrustedIdpSubView.singleLogout"),
				singleLogout);
	}

	@Override
	public List<String> getBreadcrumbs()
	{
		if (editMode)
			return Arrays.asList(msg.getMessage("EditIndividualTrustedIdpSubView.trustedIdp"),
					configBinder.getBean().getName());
		else
			return Arrays.asList(msg.getMessage("EditIndividualTrustedIdpSubView.newTrustedIdp"));
	}

	private SAMLIndividualTrustedSamlIdpConfiguration getTrustedFederation() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();

		return configBinder.getBean();
	}
}
