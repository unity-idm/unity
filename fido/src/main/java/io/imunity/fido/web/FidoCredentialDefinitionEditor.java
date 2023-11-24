/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.yubico.webauthn.data.AttestationConveyancePreference;
import com.yubico.webauthn.data.UserVerificationRequirement;
import io.imunity.fido.credential.FidoCredential;
import io.imunity.vaadin.elements.TooltipFactory;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionViewer;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;
import static java.util.Objects.isNull;


class FidoCredentialDefinitionEditor implements CredentialDefinitionEditor, CredentialDefinitionViewer
{
	private final MessageSource msg;
	private final HtmlTooltipFactory htmlTooltipFactory;

	private Select<AttestationConveyancePreference> attestationConveyance;
	private Select<UserVerificationRequirement> userVerification;
	private Checkbox loginLessAllowed;
	private TextField hostName;

	FidoCredentialDefinitionEditor(MessageSource msg, HtmlTooltipFactory htmlTooltipFactory)
	{
		this.msg = msg;
		this.htmlTooltipFactory = htmlTooltipFactory;
	}

	@Override
	public Component getEditor(String credentialDefinitionConfiguration)
	{
		attestationConveyance = new Select<>();
		attestationConveyance.setItems(AttestationConveyancePreference.values());
		attestationConveyance.setWidth(TEXT_FIELD_MEDIUM.value());
		userVerification = new Select<>();
		userVerification.setWidth(TEXT_FIELD_MEDIUM.value());
		userVerification.setItems(UserVerificationRequirement.values());
		userVerification.setItemLabelGenerator(Enum::name);
		loginLessAllowed = new Checkbox(msg.getMessage("Fido.credEditor.loginLess"));
		hostName = new TextField();
		hostName.setWidth(TEXT_FIELD_MEDIUM.value());

		FormLayout ret = new FormLayout();
		ret.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		ret.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		ret.addFormItem(attestationConveyance, msg.getMessage("Fido.credEditor.attestationConveyance"))
				.add(htmlTooltipFactory.get(msg.getMessage("Fido.credEditor.attestationConveyance.tip")));
		ret.addFormItem(userVerification, msg.getMessage("Fido.credEditor.userVerification"))
				.add(htmlTooltipFactory.get(msg.getMessage("Fido.credEditor.userVerification.tip")));
		ret.addFormItem(hostName, msg.getMessage("Fido.credEditor.hostName"))
				.add(TooltipFactory.get(msg.getMessage("Fido.credEditor.hostName.tip")));
		ret.addFormItem(loginLessAllowed, "")
				.add(htmlTooltipFactory.get(msg.getMessage("Fido.credEditor.loginLess.tip")));

		FidoCredential credential = isNull(credentialDefinitionConfiguration) ? 
				new FidoCredential() : FidoCredential.deserialize(credentialDefinitionConfiguration);
		initUIState(credential);

		return ret;
	}

	private void initUIState(FidoCredential credential)
	{
		attestationConveyance.setValue(AttestationConveyancePreference.valueOf(credential.getAttestationConveyance()));
		userVerification.setValue(UserVerificationRequirement.valueOf(credential.getUserVerification()));
		hostName.setValue(credential.getHostName());
		loginLessAllowed.setValue(credential.isLoginLessAllowed());
	}

	@Override
	public String getCredentialDefinition() throws IllegalCredentialException
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(getCredential());
		} catch (JsonProcessingException e)
		{
			throw new IllegalCredentialException("Cannot serialize credential object", e);
		}
	}

	@Override
	public Component getViewer(String credentialDefinitionConfiguration)
	{
		return getEditor(credentialDefinitionConfiguration);
	}

	private FidoCredential getCredential()
	{
		FidoCredential credential = new FidoCredential();
		credential.setAttestationConveyance(attestationConveyance.getValue().toString());
		credential.setUserVerification(userVerification.getValue().toString());
		credential.setHostName(hostName.getValue());
		credential.setLoginLessOption(loginLessAllowed.getValue());
		return credential;
	}
}
