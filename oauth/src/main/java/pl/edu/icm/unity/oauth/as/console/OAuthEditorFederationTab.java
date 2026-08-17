/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;
import static io.imunity.vaadin.elements.CssClassNames.MONOSPACE;
import static io.imunity.vaadin.elements.CssClassNames.SMALL_FONT_FIELD;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import pl.edu.icm.unity.oauth.as.token.JwksParseUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.validator.IntegerRangeValidator;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.vaadin.auth.services.ServiceEditorBase;
import io.imunity.vaadin.auth.services.ServiceEditorComponent;
import java.util.function.Consumer;

import pl.edu.icm.unity.base.message.MessageSource;

class OAuthEditorFederationTab extends VerticalLayout implements ServiceEditorBase.EditorTab
{
	private final MessageSource msg;
	private final Set<String> credentials;
	private final Set<String> validators;
	private Checkbox federationMembership;

	OAuthEditorFederationTab(MessageSource msg, Set<String> credentials, Set<String> validators)
	{
		this.msg = msg;
		this.credentials = credentials;
		this.validators = validators;
	}

	void initUI(Binder<OAuthServiceConfiguration> configBinder)
	{
		setPadding(false);

		FormLayout federationLayout = new FormLayout();
		federationLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		federationLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		federationMembership = new Checkbox(
				msg.getMessage("OAuthEditorFederationTab.federationMembershipEnabled"));
		federationLayout.addFormItem(federationMembership, "");
		configBinder.forField(federationMembership)
				.bind("federationMembershipEnabled");

		TextField trustAnchorId = new TextField();
		trustAnchorId.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(trustAnchorId)
				.withValidator(v -> !federationMembership.getValue() || (v != null && !v.isBlank()),
						msg.getMessage("fieldRequired"))
				.bind("federationTrustAnchorId");
		federationLayout.addFormItem(trustAnchorId, msg.getMessage("OAuthEditorGeneralTab.federationTrustAnchorId"));

		TextField superiorEntityId = new TextField();
		superiorEntityId.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(superiorEntityId)
				.bind("federationSuperiorEntityId");
		federationLayout.addFormItem(superiorEntityId, msg.getMessage("OAuthEditorGeneralTab.federationSuperiorEntityId"));

		TextArea jwks = new TextArea();
		jwks.setWidth(TEXT_FIELD_BIG.value());
		jwks.setHeight("14em");
		jwks.addClassName(MONOSPACE.getName());
		jwks.addClassName(SMALL_FONT_FIELD.getName());
		configBinder.forField(jwks)
				.withValidator(v -> !federationMembership.getValue() || (v != null && !v.isBlank()),
						msg.getMessage("fieldRequired"))
				.withValidator((v, c) -> {
					if (v == null || v.isEmpty())
						return ValidationResult.ok();
					Optional<String> error = JwksParseUtils.validationError(v);
					return error.map(e -> ValidationResult.error(
							msg.getMessage("OAuthEditorGeneralTab.federationJwksInvalid") + ": " + e))
							.orElseGet(ValidationResult::ok);
				})
				.bind("federationTrustAnchorJwks");
		federationLayout.addFormItem(jwks, msg.getMessage("OAuthEditorGeneralTab.federationJwks"));

		ComboBox<String> federationCredential = new ComboBox<>();
		federationCredential.setItems(credentials);
		federationCredential.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(federationCredential)
				.withValidator(v -> !federationMembership.getValue() || (v != null && !v.isBlank()),
						msg.getMessage("fieldRequired"))
				.bind("federationCredential");
		federationLayout.addFormItem(federationCredential, msg.getMessage("OAuthEditorGeneralTab.federationCredential"));

		IntegerField metadataValidity = new IntegerField();
		metadataValidity.setStepButtonsVisible(true);
		configBinder.forField(metadataValidity)
				.asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null))
				.bind("federationMetadataValidity");
		federationLayout.addFormItem(metadataValidity, msg.getMessage("OAuthEditorGeneralTab.federationMetadataValidity"));

		Select<String> federationTruststore = new Select<>();
		federationTruststore.setItems(validators);
		federationTruststore.setWidth(TEXT_FIELD_BIG.value());
		federationTruststore.setEmptySelectionAllowed(true);
		federationTruststore.setEmptySelectionCaption(msg.getMessage("TrustStore.default"));
		configBinder.forField(federationTruststore)
				.bind("federationTruststore");
		federationLayout.addFormItem(federationTruststore, msg.getMessage("OAuthEditorFederationTab.federationTruststore"));

		ComboBox<String> federationHostnameChecking = new ComboBox<>();
		federationHostnameChecking.setItems(Arrays.stream(ServerHostnameCheckingMode.values())
				.map(eu.unicore.util.httpclient.ServerHostnameCheckingMode::name).toList());
		federationHostnameChecking.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(federationHostnameChecking)
				.bind("federationHostnameChecking");
		federationLayout.addFormItem(federationHostnameChecking, msg.getMessage("OAuthEditorFederationTab.federationHostnameChecking"));

		TextField federationDisplayName = new TextField();
		federationDisplayName.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(federationDisplayName)
				.bind("federationDisplayName");
		federationLayout.addFormItem(federationDisplayName, msg.getMessage("OAuthEditorFederationTab.federationDisplayName"));

		TextField federationLogoUri = new TextField();
		federationLogoUri.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(federationLogoUri)
				.bind("federationLogoUri");
		federationLayout.addFormItem(federationLogoUri, msg.getMessage("OAuthEditorFederationTab.federationLogoUri"));

		trustAnchorId.setEnabled(false);
		superiorEntityId.setEnabled(false);
		jwks.setEnabled(false);
		federationCredential.setEnabled(false);
		metadataValidity.setEnabled(false);
		federationTruststore.setEnabled(false);
		federationHostnameChecking.setEnabled(false);
		federationDisplayName.setEnabled(false);
		federationLogoUri.setEnabled(false);

		federationMembership.addValueChangeListener(e -> {
			boolean enabled = e.getValue();
			trustAnchorId.setEnabled(enabled);
			trustAnchorId.setRequiredIndicatorVisible(enabled);
			superiorEntityId.setEnabled(enabled);
			jwks.setEnabled(enabled);
			jwks.setRequiredIndicatorVisible(enabled);
			federationCredential.setEnabled(enabled);
			federationCredential.setRequiredIndicatorVisible(enabled);
			metadataValidity.setEnabled(enabled);
			federationTruststore.setEnabled(enabled);
			federationHostnameChecking.setEnabled(enabled);
			federationDisplayName.setEnabled(enabled);
			federationLogoUri.setEnabled(enabled);
		});

		add(federationLayout);
	}

	void addFederationMembershipChangeListener(Consumer<Boolean> listener)
	{
		federationMembership.addValueChangeListener(e -> listener.accept(e.getValue()));
	}

	@Override
	public VaadinIcon getIcon()
	{
		return VaadinIcon.CONNECT;
	}

	@Override
	public String getType()
	{
		return ServiceEditorComponent.ServiceEditorTab.FEDERATION.toString();
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("OAuthEditorFederationTab.caption");
	}
}
