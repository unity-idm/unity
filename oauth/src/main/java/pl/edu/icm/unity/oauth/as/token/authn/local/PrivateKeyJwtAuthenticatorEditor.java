/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn.local;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;

import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.BaseLocalAuthenticatorEditor;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.oauth.as.token.authn.JwtClientAssertionVerifier;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

class PrivateKeyJwtAuthenticatorEditor extends BaseLocalAuthenticatorEditor implements AuthenticatorEditor
{
	private final MessageSource msg;
	private IntegerField clockSkew;

	PrivateKeyJwtAuthenticatorEditor(MessageSource msg, Collection<CredentialDefinition> credentialDefinitions)
	{
		super(msg, credentialDefinitions.stream()
				.filter(c -> c.getTypeId().equals(PrivateKeyJwtVerificator.NAME))
				.map(CredentialDefinition::getName)
				.collect(Collectors.toList()));
		this.msg = msg;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("PrivateKeyJwtAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		localCredential.setWidth(TEXT_FIELD_MEDIUM.value());

		clockSkew = new IntegerField();
		clockSkew.setStepButtonsVisible(true);
		clockSkew.setWidth(TEXT_FIELD_MEDIUM.value());
		clockSkew.setMin(0);
		clockSkew.setMax((int) JwtClientAssertionVerifier.MAX_ASSERTION_LIFETIME.toSeconds());
		clockSkew.setValue((int) JwtClientAssertionVerifier.DEFAULT_CLOCK_SKEW.toSeconds());

		FormLayout header = new FormLayout();
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));
		header.addFormItem(localCredential, msg.getMessage("BaseLocalAuthenticatorEditor.localCredential"));
		header.addFormItem(clockSkew, msg.getMessage("PrivateKeyJwtAuthenticatorEditor.clockSkew"));

		if (editMode && toEdit.configuration != null && !toEdit.configuration.isBlank())
		{
			try
			{
				PrivateKeyJwtAuthenticatorProperties props = new PrivateKeyJwtAuthenticatorProperties(
						UnityPropertiesHelper.parse(toEdit.configuration));
				localCredential.setValue(props.getValue(PrivateKeyJwtAuthenticatorProperties.CREDENTIAL_NAME));
				clockSkew.setValue(props.getIntValue(PrivateKeyJwtAuthenticatorProperties.ALLOWED_CLOCK_SKEW));
			} catch (Exception ignored)
			{
			}
		}

		return header;
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
	{
		String credName = getLocalCredential();
		Integer clockSkewValue = clockSkew.getValue();
		if (clockSkewValue == null || clockSkewValue < 0
				|| clockSkewValue > JwtClientAssertionVerifier.MAX_ASSERTION_LIFETIME.toSeconds())
			throw new FormValidationException(msg.getMessage("PrivateKeyJwtAuthenticatorEditor.invalidClockSkew",
					JwtClientAssertionVerifier.MAX_ASSERTION_LIFETIME.toSeconds()));

		Properties raw = new Properties();
		if (credName != null && !credName.isBlank())
			raw.put(PrivateKeyJwtAuthenticatorProperties.PREFIX + PrivateKeyJwtAuthenticatorProperties.CREDENTIAL_NAME,
					credName);
		raw.put(PrivateKeyJwtAuthenticatorProperties.PREFIX + PrivateKeyJwtAuthenticatorProperties.ALLOWED_CLOCK_SKEW,
				String.valueOf(clockSkewValue));
		StringWriter writer = new StringWriter();
		try
		{
			raw.store(writer, "");
		} catch (IOException e)
		{
			throw new FormValidationException("Can't serialize configuration", e);
		}
		return new AuthenticatorDefinition(getName(), PrivateKeyJwtVerificator.NAME, writer.toString(), null);
	}
}
