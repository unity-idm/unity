/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.yubico.webauthn.data.AttestationConveyancePreference;
import com.yubico.webauthn.data.UserVerificationRequirement;
import io.imunity.fido.credential.FidoCredential;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;

import static java.util.Objects.isNull;

/**
 * Basic Fido credential definition editor. Requires configuration to be added.
 *
 * @author R. Ledzinski
 */
class FidoCredentialDefinitionEditor implements CredentialDefinitionEditor, CredentialDefinitionViewer
{
	private MessageSource msg;

	private ComboBox<AttestationConveyancePreference> attestationConveyance;
	private ComboBox<UserVerificationRequirement> userVerification;
	private TextField hostName;

	public FidoCredentialDefinitionEditor(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public Component getEditor(String credentialDefinitionConfiguration)
	{
		attestationConveyance = new ComboBox<>(msg.getMessage("Fido.credEditor.attestationConveyance"));
		attestationConveyance.setItems(AttestationConveyancePreference.values());
		attestationConveyance.setEmptySelectionAllowed(false);
		attestationConveyance.setTextInputAllowed(false);
		userVerification = new ComboBox<>(msg.getMessage("Fido.credEditor.userVerification"));
		userVerification.setItems(UserVerificationRequirement.values());
		userVerification.setEmptySelectionAllowed(false);
		userVerification.setTextInputAllowed(false);
		hostName = new TextField(msg.getMessage("Fido.credEditor.hostName"));

		FormLayout ret = new CompactFormLayout(attestationConveyance, userVerification, hostName);
		ret.setSpacing(true);
		ret.setMargin(true);

		FidoCredential credential = isNull(credentialDefinitionConfiguration) ? new FidoCredential() : FidoCredential.deserialize(credentialDefinitionConfiguration);
		initUIState(credential);

		return ret;
	}

	private void initUIState(FidoCredential credential)
	{
		attestationConveyance.setValue(AttestationConveyancePreference.valueOf(credential.getAttestationConveyance()));
		userVerification.setValue(UserVerificationRequirement.valueOf(credential.getUserVerification()));
		hostName.setValue(credential.getHostName());
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
		return credential;
	}
}
