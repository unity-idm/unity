/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import com.nimbusds.oauth2.sdk.client.ClientType;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import io.imunity.vaadin.elements.CopyToClipboardButton;
import io.imunity.vaadin.elements.CustomValuesMultiSelectComboBox;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.UnitySubView;
import io.imunity.vaadin.endpoint.common.file.FileField;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.*;

/**
 * Subview for edit oauth client.
 * 
 * @author P.Piernik
 *
 */
class EditOAuthClientSubView extends VerticalLayout implements UnitySubView
{
	private final MessageSource msg;
	private final UnityServerConfiguration serverConfig;
	private final Binder<OAuthClient> binder;
	private final boolean editMode;
	private final Set<String> allClientsIds;
	private final Supplier<Set<String>> scopesSupplier;

	EditOAuthClientSubView(MessageSource msg,
			UnityServerConfiguration serverConfig, Set<String> allClientsIds, 
			Supplier<Set<String>> scopesSupplier, OAuthClient toEdit,
			Consumer<OAuthClient> onConfirm, Runnable onCancel,
			NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.serverConfig = serverConfig;
		this.allClientsIds = allClientsIds;
		this.scopesSupplier = scopesSupplier;
		
		editMode = toEdit != null;
		binder = new Binder<>(OAuthClient.class);
		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.add(buildHeaderSection());
		mainView.add(buildConsentScreenSection());

		Button cancelButton = new Button(msg.getMessage("cancel"), event -> onCancel.run());
		cancelButton.setWidthFull();
		Button updateButton = new Button(editMode ? msg.getMessage("update") :  msg.getMessage("create"), event ->
		{

			OAuthClient client;
			try
			{
				client = getOAuthClient();
			} catch (FormValidationException e)
			{
				notificationPresenter.showError(
						msg.getMessage("EditOAuthClientSubView.invalidConfiguration"), e.getMessage());
				return;
			}
			if (client.getSecret() != null && !client.getSecret().isEmpty())
			{
				ConfirmDialog confirmDialog = new ConfirmDialog();
				Button confirmButton = new Button(msg.getMessage("EditOAuthClientSubView.confirm"), e -> onConfirm.accept(client));
				confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
				confirmButton.setTooltipText(msg.getMessage("EditOAuthClientSubView.confirmTooltip"));
				Button back = new Button(msg.getMessage("EditOAuthClientSubView.goBack"));
				back.setTooltipText(msg.getMessage("EditOAuthClientSubView.goBackTooltip"));

				confirmDialog.setHeader(msg.getMessage("ConfirmDialog.confirm"));
				confirmDialog.add(new Html("<div>" + msg.getMessage("EditOAuthClientSubView.confirmAddClient") + "</div>"));
				confirmDialog.setConfirmButton(confirmButton);
				confirmDialog.setCancelable(true);
				confirmDialog.setCancelButton(back);
				confirmDialog.setWidth("30em");
				confirmDialog.setHeight("20em");
				confirmDialog.open();
			}
			else
				onConfirm.accept(client);
		});
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		updateButton.setWidthFull();
		HorizontalLayout buttonsLayout = new HorizontalLayout(cancelButton, updateButton);
		buttonsLayout.setClassName(EDIT_VIEW_ACTION_BUTTONS_LAYOUT.getName());
		mainView.add(buttonsLayout);

		binder.setBean(editMode ? toEdit.clone()
				: new OAuthClient(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
		add(mainView);
	}

	private FormLayout buildHeaderSection()
	{
		FormLayout header = new FormLayout();
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		TextField name = new TextField();
		binder.forField(name).withValidator((v, c) -> {
			if (v != null && v.length() == 1)
			{
				return ValidationResult.error(msg.getMessage("toShortValue"));
			}
			return ValidationResult.ok();

		}).bind("name");
		header.addFormItem(name, msg.getMessage("EditOAuthClientSubView.name"));

		Select<String> type = new Select<>();
		type.setItems(Stream.of(ClientType.values()).map(Enum::toString).collect(Collectors.toList()));
		type.setEmptySelectionAllowed(false);
		binder.forField(type).bind("type");
		header.addFormItem(type, msg.getMessage("EditOAuthClientSubView.type"));
		
		TextFieldWithGenerator id = new TextFieldWithGenerator();
		id.setReadOnly(editMode);
		id.setWidth(30, Unit.EM);
		binder.forField(id).asRequired(msg.getMessage("fieldRequired")).withValidator((v, c) -> {
			if (v != null && allClientsIds.contains(v))
			{
				return ValidationResult.error(msg.getMessage("EditOAuthClientSubView.invalidClientId"));
			}

			return ValidationResult.ok();
		}).bind("id");
		header.addFormItem(id, msg.getMessage("EditOAuthClientSubView.id"));
		
		CustomField<String> secret;
		if (!editMode)
		{
			secret = new TextFieldWithGenerator();
			secret.setWidth(30, Unit.EM);
			binder.forField(secret).withValidator((v, c) -> {
				if ((v == null || v.isEmpty()) && ClientType.CONFIDENTIAL.toString().equals(type.getValue()))
				{
					return ValidationResult.error(msg.getMessage("fieldRequired"));
				}
				return ValidationResult.ok();

			}).bind("secret");
			header.addFormItem(secret, msg.getMessage("EditOAuthClientSubView.secret"));
		} else
		{
			TextFieldWithChangeConfirmation<TextFieldWithGenerator> secretWithChangeConfirmation =
					new TextFieldWithChangeConfirmation<>(msg, new TextFieldWithGenerator());
			secretWithChangeConfirmation.setWidth(30, Unit.EM);
			binder.forField(secretWithChangeConfirmation).withValidator((v, c) -> {
				if (secretWithChangeConfirmation.isEditMode())
				{
					return ValidationResult.error(msg.getMessage("fieldRequired"));
				}

				return ValidationResult.ok();
			}).bind("secret");
			header.addFormItem(secretWithChangeConfirmation, msg.getMessage("EditOAuthClientSubView.secret"));
			secret = secretWithChangeConfirmation;
		}

		type.addValueChangeListener(e ->
		{
			secret.setEnabled(ClientType.CONFIDENTIAL.toString().equals(e.getValue()));
			if (!secret.isEnabled())
				secret.setValue("");
		});
		
		MultiSelectComboBox<String> allowedFlows = new MultiSelectComboBox<>();
		allowedFlows.setAutoExpand(MultiSelectComboBox.AutoExpandMode.BOTH);
		allowedFlows.setWidth(TEXT_FIELD_MEDIUM.value());
		allowedFlows.setItems(
				Stream.of(GrantFlow.values()).map(Enum::toString).collect(Collectors.toList()));
		binder.forField(allowedFlows).withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}
			return ValidationResult.ok();
		})
				.withConverter(List::copyOf, HashSet::new)
				.bind(OAuthClient::getFlows, OAuthClient::setFlows);
		header.addFormItem(allowedFlows, msg.getMessage("EditOAuthClientSubView.allowedFlows"));

		MultiSelectComboBox<String> redirectURIs = new CustomValuesMultiSelectComboBox();
		redirectURIs.setPlaceholder(msg.getMessage("typeAndConfirm"));
		redirectURIs.setWidth(TEXT_FIELD_BIG.value());
		binder.forField(redirectURIs).withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}
			return ValidationResult.ok();
		})
				.withConverter(List::copyOf, HashSet::new)
				.bind(OAuthClient::getRedirectURIs, OAuthClient::setRedirectURIs);
		header.addFormItem(redirectURIs, msg.getMessage("EditOAuthClientSubView.authorizedRedirectURIs"));

		Set<String> definedScopes = scopesSupplier.get();

		MultiSelectComboBox<String> allowedScopes = new MultiSelectComboBox<>();
		allowedScopes.setItems(definedScopes);
		binder.forField(allowedScopes).withValidator((v, c) -> {
			if (v != null && !v.isEmpty() && !definedScopes.containsAll(v))
			{
				return ValidationResult.error(msg.getMessage("EditOAuthClientSubView.invalidAllowedScopes"));
			}

			return ValidationResult.ok();
		})
				.withConverter(List::copyOf, HashSet::new)
				.bind(OAuthClient::getScopes, OAuthClient::setScopes);
		
		Checkbox allowAllScopes = new Checkbox(msg.getMessage("EditOAuthClientSubView.allowAllScopes"));
		binder.forField(allowAllScopes).bind("allowAnyScopes");
		allowAllScopes.addValueChangeListener(v -> allowedScopes.setEnabled(!v.getValue()));
		
		header.addFormItem(allowAllScopes, "");

		return header;
	}

	private Component buildConsentScreenSection()
	{
		FormLayout consentScreenL = new FormLayout();
		consentScreenL.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		consentScreenL.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		TextField title = new TextField();
		binder.forField(title).bind("title");
		consentScreenL.addFormItem(title, msg.getMessage("EditOAuthClientSubView.title"));

		FileField logo = new FileField(msg, "image/*", "logo.jpg", serverConfig.getFileSizeLimit());
		logo.configureBinding(binder, "logo");
		consentScreenL.addFormItem(logo, msg.getMessage("EditOAuthProviderSubView.logo"));

		AccordionPanel consentScreenSection = new AccordionPanel(
				msg.getMessage("EditOAuthClientSubView.consentScreen"), consentScreenL);
		consentScreenSection.setWidthFull();
		consentScreenSection.setOpened(true);
		return consentScreenSection;
	}

	private OAuthClient getOAuthClient() throws FormValidationException
	{
		if (binder.validate().hasErrors())
			throw new FormValidationException();

		return binder.getBean();
	}

	@Override
	public List<String> getBreadcrumbs()
	{
		if (editMode)
			return Arrays.asList(msg.getMessage("EditOAuthClientSubView.client"), binder.getBean().getId());
		else
			return Collections.singletonList(msg.getMessage("EditOAuthClientSubView.newClient"));
	}

	private class TextFieldWithGenerator extends CustomField<String>
	{
		private final TextField field;

		public TextFieldWithGenerator()
		{
			field = new TextField();
			field.addValueChangeListener(
					e -> fireEvent(new AbstractField.ComponentValueChangeEvent<>(this, this, field.getValue(), true)));
			add(initContent());
		}

		@Override
		protected String generateModelValue()
		{
			return getValue();
		}

		@Override
		protected void setPresentationValue(String s)
		{
			setValue(s);
		}

		@Override
		public String getValue()
		{
			return field.getValue();
		}

		private Component initContent()
		{
			HorizontalLayout main = new HorizontalLayout();
			main.add(field);

			CopyToClipboardButton copy = new CopyToClipboardButton(msg::getMessage, field);
			copy.addClassName(POINTER.getName());
			main.add(copy);

			Icon gen = VaadinIcon.COGS.create();
			gen.addClassName(POINTER.getName());
			gen.addClickListener(e -> field.setValue(UUID.randomUUID().toString()));
			gen.setTooltipText(msg.getMessage("EditOAuthClientSubView.generate"));
			main.add(new Div(gen));
			return main;

		}

		@Override
		public void setValue(String value)
		{
			field.setValue(value);
		}

		@Override
		public void setWidth(float width, Unit unit)
		{
			if (field != null)
			{
				field.setWidth(width, unit);
			} else
			{
				super.setWidth(width, unit);
			}
		}
	}

}
