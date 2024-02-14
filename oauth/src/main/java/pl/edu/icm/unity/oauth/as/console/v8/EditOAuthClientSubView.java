/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console.v8;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.nimbusds.oauth2.sdk.client.ClientType;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.clipboard.CopyToClipboardButton;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.TextFieldWithChangeConfirmation;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.common.file.ImageField;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

/**
 * Subview for edit oauth client.
 * 
 * @author P.Piernik
 *
 */
class EditOAuthClientSubView extends CustomComponent implements UnitySubView
{
	private MessageSource msg;
	private URIAccessService uriAccessService;
	private UnityServerConfiguration serverConfig;
	private Binder<OAuthClient> binder;
	private boolean editMode = false;
	private Set<String> allClientsIds;
	private Supplier<Set<String>> scopesSupplier;

	EditOAuthClientSubView(MessageSource msg, URIAccessService uriAccessService,
			UnityServerConfiguration serverConfig, Set<String> allClientsIds, 
			Supplier<Set<String>> scopesSupplier, OAuthClient toEdit,
			Consumer<OAuthClient> onConfirm, Runnable onCancel)
	{
		this.msg = msg;
		this.uriAccessService = uriAccessService;
		this.serverConfig = serverConfig;
		this.allClientsIds = allClientsIds;
		this.scopesSupplier = scopesSupplier;
		
		editMode = toEdit != null;
		binder = new Binder<>(OAuthClient.class);
		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(buildHeaderSection());
		mainView.addComponent(buildConsentScreenSection());
		Runnable onConfirmR = () -> {

			OAuthClient client;
			try
			{
				client = getOAuthClient();
			} catch (FormValidationException e)
			{
				NotificationPopup.showError(msg,
						msg.getMessage("EditOAuthClientSubView.invalidConfiguration"), e);
				return;
			}
			if (client.getSecret() != null && !client.getSecret().isEmpty())
			{
				ConfirmDialog confirmDialog = new ConfirmDialog(msg,
						msg.getMessage("EditOAuthClientSubView.confirmAddClient"),
						msg.getMessage("EditOAuthClientSubView.confirm"),
						msg.getMessage("EditOAuthClientSubView.confirmTooltip"),
						msg.getMessage("EditOAuthClientSubView.goBack"),
						msg.getMessage("EditOAuthClientSubView.goBackTooltip"), () -> {
							onConfirm.accept(client);
						});
				confirmDialog.setHTMLContent(true);
				confirmDialog.setSizeEm(30, 20);
				confirmDialog.show();
				
			} else
			{
				onConfirm.accept(client);
			}
		};
		mainView.addComponent(editMode
				? StandardButtonsHelper.buildConfirmEditButtonsBar(msg, onConfirmR, onCancel)
				: StandardButtonsHelper.buildConfirmNewButtonsBar(msg, onConfirmR, onCancel));

		binder.setBean(editMode ? toEdit.clone()
				: new OAuthClient(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
		setCompositionRoot(mainView);
	}

	private FormLayoutWithFixedCaptionWidth buildHeaderSection()
	{
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);

		TextField name = new TextField();
		name.setCaption(msg.getMessage("EditOAuthClientSubView.name"));
		binder.forField(name).withValidator((v, c) -> {
			if (v != null && !v.isEmpty() && v.length() < 2)
			{
				return ValidationResult.error(msg.getMessage("toShortValue"));
			}
			return ValidationResult.ok();

		}).bind("name");
		header.addComponent(name);

		ComboBox<String> type = new ComboBox<>();
		type.setCaption(msg.getMessage("EditOAuthClientSubView.type"));
		type.setItems(Stream.of(ClientType.values()).map(f -> f.toString()).collect(Collectors.toList()));
		type.setEmptySelectionAllowed(false);
		binder.forField(type).bind("type");
		header.addComponent(type);
		
		TextFieldWithGenerator id = new TextFieldWithGenerator();
		id.setCaption(msg.getMessage("EditOAuthClientSubView.id"));
		id.setReadOnly(editMode);
		id.setWidth(30, Unit.EM);
		binder.forField(id).asRequired(msg.getMessage("fieldRequired")).withValidator((v, c) -> {
			if (v != null && allClientsIds.contains(v))
			{
				return ValidationResult.error(msg.getMessage("EditOAuthClientSubView.invalidClientId"));
			}

			return ValidationResult.ok();
		}).bind("id");
		header.addComponent(id);
		
		CustomField<String> secret;
		if (!editMode)
		{
			secret = new TextFieldWithGenerator();
			secret.setCaption(msg.getMessage("EditOAuthClientSubView.secret"));
			secret.setWidth(30, Unit.EM);
			binder.forField(secret).withValidator((v, c) -> {
				if ((v == null || v.isEmpty()) && ClientType.CONFIDENTIAL.toString().equals(type.getValue()))
				{
					return ValidationResult.error(msg.getMessage("fieldRequired"));
				}
				return ValidationResult.ok();

			}).bind("secret");
			header.addComponent(secret);
		} else
		{
			TextFieldWithChangeConfirmation<TextFieldWithGenerator> secretWithChangeConfirmation = 
					new TextFieldWithChangeConfirmation<>(msg, new TextFieldWithGenerator());
			secretWithChangeConfirmation.setCaption(msg.getMessage("EditOAuthClientSubView.secret"));
			secretWithChangeConfirmation.setWidth(30, Unit.EM);
			binder.forField(secretWithChangeConfirmation).withValidator((v, c) -> {
				if (secretWithChangeConfirmation.isEditMode())
				{
					return ValidationResult.error(msg.getMessage("fieldRequired"));
				}

				return ValidationResult.ok();
			}).bind("secret");
			header.addComponent(secretWithChangeConfirmation);		
			secret = secretWithChangeConfirmation;
		}

		type.addValueChangeListener(e ->
		{
			secret.setEnabled(ClientType.CONFIDENTIAL.toString().equals(e.getValue()));
			if (!secret.isEnabled())
				secret.setValue("");
		});
		
		ChipsWithDropdown<String> allowedFlows = new ChipsWithDropdown<>();
		allowedFlows.setCaption(msg.getMessage("EditOAuthClientSubView.allowedFlows"));
		allowedFlows.setItems(
				Stream.of(GrantFlow.values()).map(f -> f.toString()).collect(Collectors.toList()));
		binder.forField(allowedFlows).withValidator((v, c) -> {
			if (v == null || v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}
			return ValidationResult.ok();
		}).bind("flows");
		header.addComponent(allowedFlows);

		ChipsWithTextfield redirectURIs = new ChipsWithTextfield(msg);
		redirectURIs.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		redirectURIs.setCaption(msg.getMessage("EditOAuthClientSubView.authorizedRedirectURIs"));
		binder.forField(redirectURIs).withValidator((v, c) -> {
			if (v == null || v.size() == 0)
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			}
			return ValidationResult.ok();
		}).bind("redirectURIs");
		header.addComponent(redirectURIs);

		Set<String> definedScopes = scopesSupplier.get();
		
		ChipsWithDropdown<String> allowedScopes = new ChipsWithDropdown<>();
		allowedScopes.setCaption(msg.getMessage("EditOAuthClientSubView.allowedScopes"));
		allowedScopes.setItems(definedScopes);
		allowedScopes.setSkipRemoveInvalidSelections(true);
		binder.forField(allowedScopes).withValidator((v, c) -> {
			if (v != null && !v.isEmpty() && !definedScopes.containsAll(v))
			{
				return ValidationResult.error(msg.getMessage("EditOAuthClientSubView.invalidAllowedScopes"));
			}

			return ValidationResult.ok();
		}).bind("scopes");
		
		CheckBox allowAllScopes = new CheckBox(msg.getMessage("EditOAuthClientSubView.allowAllScopes"));
		binder.forField(allowAllScopes).bind("allowAnyScopes");
		allowAllScopes.addValueChangeListener(v -> allowedScopes.setEnabled(!v.getValue()));
		
		header.addComponent(allowAllScopes);
		header.addComponent(allowedScopes);
		
		return header;
	}

	private Component buildConsentScreenSection()
	{
		FormLayoutWithFixedCaptionWidth consentScreenL = new FormLayoutWithFixedCaptionWidth();
		consentScreenL.setMargin(false);

		TextField title = new TextField();
		title.setCaption(msg.getMessage("EditOAuthClientSubView.title"));
		binder.forField(title).bind("title");
		consentScreenL.addComponent(title);

		ImageField logo = new ImageField(msg, uriAccessService, serverConfig.getFileSizeLimit());
		logo.setCaption(msg.getMessage("EditOAuthProviderSubView.logo"));
		logo.configureBinding(binder, "logo");
		consentScreenL.addComponent(logo);

		CollapsibleLayout consentScreenSection = new CollapsibleLayout(
				msg.getMessage("EditOAuthClientSubView.consentScreen"), consentScreenL);
		consentScreenSection.expand();
		return consentScreenSection;
	}

	private OAuthClient getOAuthClient() throws FormValidationException
	{
		if (binder.validate().hasErrors())
			throw new FormValidationException();

		return binder.getBean();
	}

	@Override
	public List<String> getBredcrumbs()
	{
		if (editMode)
			return Arrays.asList(msg.getMessage("EditOAuthClientSubView.client"), binder.getBean().getId());
		else
			return Arrays.asList(msg.getMessage("EditOAuthClientSubView.newClient"));
	}

	private class TextFieldWithGenerator extends CustomField<String>
	{
		private TextField field;

		public TextFieldWithGenerator()
		{
			field = new TextField();
			field.addValueChangeListener(
					e -> fireEvent(new ValueChangeEvent<String>(this, field.getValue(), true)));
		}

		@Override
		public String getValue()
		{
			return field.getValue();
		}

		@Override
		protected Component initContent()
		{
			HorizontalLayout main = new HorizontalLayout();
			main.addComponent(field);

			CopyToClipboardButton copy = new CopyToClipboardButton(msg, field);
			main.addComponent(copy);
			main.setComponentAlignment(copy, Alignment.MIDDLE_LEFT);

			Button gen = new Button();
			gen.addClickListener(e -> field.setValue(UUID.randomUUID().toString()));
			gen.setDescription(msg.getMessage("EditOAuthClientSubView.generate"));
			gen.setIcon(Images.cogs.getResource());
			gen.setStyleName(Styles.vButtonLink.toString());
			gen.addStyleName(Styles.vButtonBorderless.toString());
			gen.addStyleName(Styles.link.toString());
			main.addComponent(gen);
			main.setComponentAlignment(gen, Alignment.MIDDLE_LEFT);

			return main;

		}

		@Override
		protected void doSetValue(String value)
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
