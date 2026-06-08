/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.jwk.JWKSet;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.BaseAuthenticatorEditor;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

class FederatedPrivateKeyJwtAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private final MessageSource msg;
	private final PKIManagement pkiMan;
	private TextField federationTrustAnchorId;
	private TextArea federationTrustAnchorJwks;
	private ComboBox<String> federationTruststore;
	private ComboBox<String> federationHostnameChecking;

	FederatedPrivateKeyJwtAuthenticatorEditor(MessageSource msg, PKIManagement pkiMan)
	{
		super(msg);
		this.msg = msg;
		this.pkiMan = pkiMan;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("FederatedPrivateKeyJwtAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		FormLayout form = new FormLayout();
		form.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		form.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));

		federationTrustAnchorId = new TextField();
		federationTrustAnchorId.setWidth(TEXT_FIELD_BIG.value());
		federationTrustAnchorId.setClearButtonVisible(true);
		form.addFormItem(federationTrustAnchorId,
				msg.getMessage("FederatedPrivateKeyJwtAuthenticatorEditor.federationTrustAnchorId"));

		federationTrustAnchorJwks = new TextArea();
		federationTrustAnchorJwks.setWidth(TEXT_FIELD_BIG.value());
		federationTrustAnchorJwks.setHeight("8em");
		federationTrustAnchorJwks.setClearButtonVisible(true);
		form.addFormItem(federationTrustAnchorJwks,
				msg.getMessage("FederatedPrivateKeyJwtAuthenticatorEditor.federationTrustAnchorJwks"));

		federationTruststore = new ComboBox<>();
		federationTruststore.setItems(getValidatorNames());
		federationTruststore.setClearButtonVisible(true);
		federationTruststore.setWidth(TEXT_FIELD_BIG.value());
		form.addFormItem(federationTruststore,
				msg.getMessage("FederatedPrivateKeyJwtAuthenticatorEditor.federationTruststore"));

		federationHostnameChecking = new ComboBox<>();
		federationHostnameChecking.setItems(Arrays.stream(ServerHostnameCheckingMode.values())
				.map(ServerHostnameCheckingMode::name).toList());
		federationHostnameChecking.setClearButtonVisible(true);
		form.addFormItem(federationHostnameChecking,
				msg.getMessage("FederatedPrivateKeyJwtAuthenticatorEditor.federationHostnameChecking"));

		if (editMode && toEdit.configuration != null && !toEdit.configuration.isBlank())
			loadConfig(toEdit.configuration);

		return form;
	}

	private Set<String> getValidatorNames()
	{
		try
		{
			return pkiMan.getValidatorNames();
		} catch (EngineException e)
		{
			return Set.of();
		}
	}

	private void loadConfig(String configuration)
	{
		try
		{
			ObjectNode root = JsonUtil.parse(configuration);
			if (root.has("federationTrustAnchorId"))
				federationTrustAnchorId.setValue(root.get("federationTrustAnchorId").asText(""));
			if (root.has("federationTrustAnchorJwks"))
				federationTrustAnchorJwks.setValue(root.get("federationTrustAnchorJwks").asText(""));
			if (root.has("federationTruststore"))
				federationTruststore.setValue(root.get("federationTruststore").asText(""));
			if (root.has("federationHostnameChecking"))
				federationHostnameChecking.setValue(root.get("federationHostnameChecking").asText(""));
		} catch (Exception ignored)
		{
		}
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
	{
		String jwksValue = federationTrustAnchorJwks.getValue();
		if (!jwksValue.isBlank())
		{
			try
			{
				JWKSet.parse(jwksValue);
			} catch (ParseException e)
			{
				throw new FormValidationException(msg.getMessage(
						"FederatedPrivateKeyJwtAuthenticatorEditor.federationTrustAnchorJwksInvalid"));
			}
		}

		ObjectNode root = JsonUtil.parse("{}");
		String trustAnchorId = federationTrustAnchorId.getValue();
		String truststore = federationTruststore.getValue();
		String hostnameChecking = federationHostnameChecking.getValue();

		if (!trustAnchorId.isBlank())
			root.put("federationTrustAnchorId", trustAnchorId);
		if (!jwksValue.isBlank())
			root.put("federationTrustAnchorJwks", jwksValue);
		if (truststore != null && !truststore.isBlank())
			root.put("federationTruststore", truststore);
		if (hostnameChecking != null && !hostnameChecking.isBlank())
			root.put("federationHostnameChecking", hostnameChecking);

		return new AuthenticatorDefinition(getName(), FederatedPrivateKeyJwtVerificator.NAME,
				JsonUtil.serialize(root), null);
	}
}
