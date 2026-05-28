/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.IntegerRangeValidator;

import io.imunity.vaadin.auth.services.ServiceEditorBase;
import io.imunity.vaadin.auth.services.ServiceEditorComponent;
import pl.edu.icm.unity.base.message.MessageSource;

class OAuthEditorFederationTab extends VerticalLayout implements ServiceEditorBase.EditorTab
{
	private final MessageSource msg;
	private final Set<String> credentials;
	private Binder<OAuthServiceConfiguration> configBinder;

	OAuthEditorFederationTab(MessageSource msg, Set<String> credentials)
	{
		this.msg = msg;
		this.credentials = credentials;
	}

	void initUI(Binder<OAuthServiceConfiguration> configBinder)
	{
		this.configBinder = configBinder;
		setPadding(false);

		FormLayout federationLayout = new FormLayout();
		federationLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		federationLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		TextField trustAnchorId = new TextField();
		trustAnchorId.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(trustAnchorId)
				.bind("federationTrustAnchorId");
		federationLayout.addFormItem(trustAnchorId, msg.getMessage("OAuthEditorGeneralTab.federationTrustAnchorId"));

		TextArea jwks = new TextArea();
		jwks.setWidth(TEXT_FIELD_BIG.value());
		jwks.setHeight("8em");
		configBinder.forField(jwks)
				.withValidator(v -> {
					if (v == null || v.isEmpty())
						return true;
					try
					{
						com.nimbusds.jose.jwk.JWKSet.parse(v);
						return true;
					} catch (java.text.ParseException e)
					{
						return false;
					}
				}, msg.getMessage("OAuthEditorGeneralTab.federationJwksInvalid"))
				.bind("federationTrustAnchorJwks");
		federationLayout.addFormItem(jwks, msg.getMessage("OAuthEditorGeneralTab.federationJwks"));

		ComboBox<String> federationCredential = new ComboBox<>();
		federationCredential.setItems(credentials);
		federationCredential.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(federationCredential)
				.bind("federationCredential");
		federationLayout.addFormItem(federationCredential, msg.getMessage("OAuthEditorGeneralTab.federationCredential"));

		TextField superiorEntityId = new TextField();
		superiorEntityId.setWidth(TEXT_FIELD_BIG.value());
		configBinder.forField(superiorEntityId)
				.bind("federationSuperiorEntityId");
		federationLayout.addFormItem(superiorEntityId, msg.getMessage("OAuthEditorGeneralTab.federationSuperiorEntityId"));

		IntegerField metadataValidity = new IntegerField();
		metadataValidity.setStepButtonsVisible(true);
		configBinder.forField(metadataValidity)
				.asRequired(msg.getMessage("notAPositiveNumber"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("notAPositiveNumber"), 1, null))
				.bind("federationMetadataValidity");
		federationLayout.addFormItem(metadataValidity, msg.getMessage("OAuthEditorGeneralTab.federationMetadataValidity"));

		add(federationLayout);
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
