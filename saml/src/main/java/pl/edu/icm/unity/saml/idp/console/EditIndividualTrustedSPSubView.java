/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.elements.NoSpaceValidator;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.api.UnitySubView;
import io.imunity.vaadin.endpoint.common.file.FileField;
import org.bouncycastle.asn1.x500.X500Name;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.*;
import java.util.function.Consumer;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.EDIT_VIEW_ACTION_BUTTONS_LAYOUT;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

/**
 * View for edit SAML individual trusted SP
 * 
 * @author P.Piernik
 *
 */
class EditIndividualTrustedSPSubView extends VerticalLayout implements UnitySubView
{
	private final MessageSource msg;
	private final UnityServerConfiguration serverConfig;
	private final Binder<SAMLIndividualTrustedSPConfiguration> configBinder;
	private final boolean editMode;
	private final Set<String> certificates;
	private final Set<String> usedNames;

	EditIndividualTrustedSPSubView(MessageSource msg, UnityServerConfiguration serverConfig,
			URIAccessService uriAccessService, SAMLIndividualTrustedSPConfiguration toEdit,
			SubViewSwitcher subViewSwitcher, Set<String> usedNames, Set<String> certificates,
			Consumer<SAMLIndividualTrustedSPConfiguration> onConfirm, Runnable onCancel, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.certificates = certificates;
		this.usedNames = usedNames;
		this.serverConfig = serverConfig;
		editMode = toEdit != null;

		configBinder = new Binder<>(SAMLIndividualTrustedSPConfiguration.class);
		FormLayout header = buildHeaderSection();
		AccordionPanel singleLogout = buildSingleLogoutSection();
		singleLogout.setWidthFull();
		configBinder.setBean(editMode ? toEdit.clone() : new SAMLIndividualTrustedSPConfiguration());
		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.add(header);
		mainView.add(singleLogout);
		Button cancelButton = new Button(msg.getMessage("cancel"), event -> onCancel.run());
		cancelButton.setWidthFull();
		Button updateButton = new Button(editMode ? msg.getMessage("update") :  msg.getMessage("create"), event ->
		{
			try
			{
				onConfirm.accept(getTrustedFederation());
			} catch (FormValidationException e)
			{
				notificationPresenter.showError(
						msg.getMessage("EditIndividualTrustedSPSubView.invalidConfiguration"),
						e.getMessage());
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
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		TextField name = new TextField();
		configBinder.forField(name).asRequired(msg.getMessage("fieldRequired"))
				.withValidator(new NoSpaceValidator(msg::getMessage)).withValidator((s, c) -> {
					if (usedNames.contains(s))
					{
						return ValidationResult.error(msg.getMessage(
								"EditIndividualTrustedSPSubView.nameExists"));
					} else
					{
						return ValidationResult.ok();
					}

				}).bind(SAMLIndividualTrustedSPConfiguration::getName, SAMLIndividualTrustedSPConfiguration::setName);
		header.addFormItem(name, msg.getMessage("EditIndividualTrustedSPSubView.name"));

		LocalizedTextFieldDetails displayedName = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		configBinder.forField(displayedName)
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.bind(SAMLIndividualTrustedSPConfiguration::getDisplayedName, SAMLIndividualTrustedSPConfiguration::setDisplayedName);
		header.addFormItem(displayedName, msg.getMessage("EditIndividualTrustedSPSubView.displayedName"));

		FileField logo = new FileField(msg, "image/*", "logo.jpg", serverConfig.getFileSizeLimit());
		logo.setEnabled(true);
		logo.configureBinding(configBinder, "logo");
		header.addFormItem(logo, msg.getMessage("EditIndividualTrustedSPSubView.logo"));

		Checkbox x500Name = new Checkbox(msg.getMessage("EditIndividualTrustedSPSubView.X500NameUse"));
		configBinder.forField(x500Name).bind(SAMLIndividualTrustedSPConfiguration::isX500Name, SAMLIndividualTrustedSPConfiguration::setX500Name);
		header.addFormItem(x500Name, "");

		TextField id = new TextField();
		id.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(id).asRequired(msg.getMessage("fieldRequired")).withValidator((s, c) -> {
			if (x500Name.getValue())
			{
				try
				{
					new X500Name(s);
					return ValidationResult.ok();
				} catch (Exception e)
				{
					return ValidationResult.error(
							msg.getMessage("EditIndividualTrustedSPSubView.invalidX500Name"));
				}
			} else
			{
				return ValidationResult.ok();
			}
		}).bind(SAMLIndividualTrustedSPConfiguration::getId, SAMLIndividualTrustedSPConfiguration::setId);
		header.addFormItem(id, msg.getMessage("EditIndividualTrustedSPSubView.id"));

		MultiSelectComboBox<String> certificatesCombo = new MultiSelectComboBox<>();
		certificatesCombo.setItems(new ArrayList<>(certificates));
		configBinder.forField(certificatesCombo)
				.withConverter(List::copyOf, HashSet::new)
				.bind(SAMLIndividualTrustedSPConfiguration::getCertificates, SAMLIndividualTrustedSPConfiguration::setCertificates);
		header.addFormItem(certificatesCombo, msg.getMessage("EditIndividualTrustedSPSubView.certificates"));

		Checkbox encryptAssertions = new Checkbox(
				msg.getMessage("EditIndividualTrustedSPSubView.encryptAssertions"));
		configBinder.forField(encryptAssertions)
				.bind(SAMLIndividualTrustedSPConfiguration::isEncryptAssertions, SAMLIndividualTrustedSPConfiguration::setEncryptAssertions);
		header.addFormItem(encryptAssertions, "");

		MultiSelectComboBox<String> authorizedURIs = new CustomValuesMultiSelectComboBox();
		authorizedURIs.setPlaceholder(msg.getMessage("typeAndConfirm"));
		authorizedURIs.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(authorizedURIs).withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}
			return ValidationResult.ok();
		}

		)
				.withConverter(List::copyOf, HashSet::new)
				.bind(SAMLIndividualTrustedSPConfiguration::getAuthorizedRedirectsUri, SAMLIndividualTrustedSPConfiguration::setAuthorizedRedirectsUri);
		header.addFormItem(authorizedURIs, msg.getMessage("EditIndividualTrustedSPSubView.authorizedURIs"));

		return header;
	}

	private AccordionPanel buildSingleLogoutSection()
	{
		FormLayout singleLogout = new FormLayout();
		singleLogout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		singleLogout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		TextField postLogoutEndpoint = new TextField();
		postLogoutEndpoint.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(postLogoutEndpoint)
				.bind(SAMLIndividualTrustedSPConfiguration::getPostLogoutEndpoint, SAMLIndividualTrustedSPConfiguration::setPostLogoutEndpoint);
		singleLogout.addFormItem(postLogoutEndpoint, msg.getMessage("EditIndividualTrustedSPSubView.postLogoutEndpoint"));

		TextField postLogoutResponseEndpoint = new TextField();
		postLogoutResponseEndpoint.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(postLogoutResponseEndpoint)
				.bind(SAMLIndividualTrustedSPConfiguration::getPostLogoutResponseEndpoint, SAMLIndividualTrustedSPConfiguration::setPostLogoutResponseEndpoint);
		singleLogout.addFormItem(postLogoutResponseEndpoint, msg.getMessage("EditIndividualTrustedSPSubView.postLogoutResponseEndpoint"));

		TextField redirectLogoutEndpoint = new TextField();
		redirectLogoutEndpoint.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(redirectLogoutEndpoint)
				.bind(SAMLIndividualTrustedSPConfiguration::getRedirectLogoutEndpoint, SAMLIndividualTrustedSPConfiguration::setRedirectLogoutEndpoint);
		singleLogout.addFormItem(redirectLogoutEndpoint, msg.getMessage("EditIndividualTrustedSPSubView.redirectLogoutEndpoint"));

		TextField redirectLogoutResponseEndpoint = new TextField();
		redirectLogoutResponseEndpoint.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(redirectLogoutResponseEndpoint)
				.bind(SAMLIndividualTrustedSPConfiguration::getRedirectLogoutResponseEndpoint, SAMLIndividualTrustedSPConfiguration::setRedirectLogoutResponseEndpoint);
		singleLogout.addFormItem(redirectLogoutResponseEndpoint, msg.getMessage("EditIndividualTrustedSPSubView.redirectLogoutResponseEndpoint"));

		TextField soapLogoutEndpoint = new TextField();
		soapLogoutEndpoint.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(soapLogoutEndpoint)
				.bind(SAMLIndividualTrustedSPConfiguration::getSoapLogoutEndpoint, SAMLIndividualTrustedSPConfiguration::setSoapLogoutEndpoint);
		singleLogout.addFormItem(soapLogoutEndpoint, msg.getMessage("EditIndividualTrustedSPSubView.soapLogoutEndpoint"));

		return new AccordionPanel(msg.getMessage("EditIndividualTrustedSPSubView.singleLogout"),
				singleLogout);
	}

	@Override
	public List<String> getBreadcrumbs()
	{
		if (editMode)
			return Arrays.asList(msg.getMessage("EditIndividualTrustedSPSubView.trustedSP"),
					configBinder.getBean().getName());
		else
			return Collections.singletonList(msg.getMessage("EditIndividualTrustedSPSubView.newTrustedSP"));
	}

	private SAMLIndividualTrustedSPConfiguration getTrustedFederation() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();

		return configBinder.getBean();
	}
}
