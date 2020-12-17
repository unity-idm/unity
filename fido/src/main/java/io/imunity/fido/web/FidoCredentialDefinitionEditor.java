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
import org.apache.commons.lang3.StringUtils;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static io.imunity.tooltip.TooltipExtension.tooltip;
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
	private TextField origins;
	private TextField allowSubdomains;

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
		tooltip(attestationConveyance, msg.getMessage("Fido.credEditor.attestationConveyance.tip"));

		userVerification = new ComboBox<>(msg.getMessage("Fido.credEditor.userVerification"));
		userVerification.setItems(UserVerificationRequirement.values());
		userVerification.setEmptySelectionAllowed(false);
		userVerification.setTextInputAllowed(false);
		tooltip(userVerification, msg.getMessage("Fido.credEditor.userVerification.tip"));

		hostName = new TextField(msg.getMessage("Fido.credEditor.hostName"));
		tooltip(hostName, msg.getMessage("Fido.credEditor.hostName.tip"));

		allowSubdomains = new TextField("Subdomains");

		origins = new TextField("Origins");

		FormLayout ret = new FormLayout(attestationConveyance, userVerification, hostName, allowSubdomains, origins);
		ret.setMargin(true);
		
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
		allowSubdomains.setValue(credential.isAllowSubdomains() ? "true" : "false");
		origins.setValue(isNull(credential.getAllowedOrigins()) || credential.getAllowedOrigins().isEmpty() ? "" : String.join(";", credential.getAllowedOrigins()));
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
		Set<String> set = new HashSet<>();
		for(String s : origins.getValue().split(";"))
			if (StringUtils.isNotBlank(s))
				set.add(s);
		credential.setAllowedOrigins(set);
		credential.setAllowSubdomains(allowSubdomains.getValue().equalsIgnoreCase("true"));
		return credential;
	}
}
