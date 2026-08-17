/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn.federation;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;

import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.BaseAuthenticatorEditor;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.oauth.as.token.authn.JwtClientAssertionVerifier;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

class FederatedPrivateKeyJwtAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private final MessageSource msg;
	private IntegerField clockSkew;
	private IntegerField maxAssertionLifetime;

	FederatedPrivateKeyJwtAuthenticatorEditor(MessageSource msg)
	{
		super(msg);
		this.msg = msg;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		init(msg.getMessage("FederatedPrivateKeyJwtAuthenticatorEditor.defaultName"), toEdit, forceNameEditable);

		clockSkew = new IntegerField();
		clockSkew.setStepButtonsVisible(true);
		clockSkew.setWidth(TEXT_FIELD_MEDIUM.value());
		clockSkew.setMin(0);
		clockSkew.setValue((int) JwtClientAssertionVerifier.DEFAULT_CLOCK_SKEW.toSeconds());

		maxAssertionLifetime = new IntegerField();
		maxAssertionLifetime.setStepButtonsVisible(true);
		maxAssertionLifetime.setWidth(TEXT_FIELD_MEDIUM.value());
		maxAssertionLifetime.setMin(1);
		maxAssertionLifetime.setValue((int) JwtClientAssertionVerifier.DEFAULT_MAX_ASSERTION_LIFETIME.toSeconds());

		FormLayout form = new FormLayout();
		form.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		form.addFormItem(name, msg.getMessage("BaseAuthenticatorEditor.name"));
		form.addFormItem(clockSkew, msg.getMessage("PrivateKeyJwtAuthenticatorEditor.clockSkew"));
		form.addFormItem(maxAssertionLifetime, msg.getMessage("PrivateKeyJwtAuthenticatorEditor.maxAssertionLifetime"));

		if (toEdit != null && toEdit.configuration != null && !toEdit.configuration.isBlank())
		{
			try
			{
				FederatedPrivateKeyJwtAuthenticatorProperties props = new FederatedPrivateKeyJwtAuthenticatorProperties(
						UnityPropertiesHelper.parse(toEdit.configuration));
				clockSkew.setValue(props.getIntValue(FederatedPrivateKeyJwtAuthenticatorProperties.ALLOWED_CLOCK_SKEW));
				maxAssertionLifetime.setValue(
						props.getIntValue(FederatedPrivateKeyJwtAuthenticatorProperties.MAX_ASSERTION_LIFETIME));
			} catch (Exception ignored)
			{
			}
		}

		return form;
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefinition() throws FormValidationException
	{
		Integer clockSkewValue = clockSkew.getValue();
		if (clockSkewValue == null || clockSkewValue < 0)
			throw new FormValidationException(msg.getMessage("PrivateKeyJwtAuthenticatorEditor.invalidClockSkew"));

		Integer maxAssertionLifetimeValue = maxAssertionLifetime.getValue();
		if (maxAssertionLifetimeValue == null || maxAssertionLifetimeValue < 1)
			throw new FormValidationException(
					msg.getMessage("PrivateKeyJwtAuthenticatorEditor.invalidMaxAssertionLifetime"));

		Properties raw = new Properties();
		raw.put(FederatedPrivateKeyJwtAuthenticatorProperties.PREFIX
				+ FederatedPrivateKeyJwtAuthenticatorProperties.ALLOWED_CLOCK_SKEW,
				String.valueOf(clockSkewValue));
		raw.put(FederatedPrivateKeyJwtAuthenticatorProperties.PREFIX
				+ FederatedPrivateKeyJwtAuthenticatorProperties.MAX_ASSERTION_LIFETIME,
				String.valueOf(maxAssertionLifetimeValue));
		StringWriter writer = new StringWriter();
		try
		{
			raw.store(writer, "");
		} catch (IOException e)
		{
			throw new FormValidationException("Can't serialize configuration", e);
		}
		return new AuthenticatorDefinition(getName(), FederatedPrivateKeyJwtVerificator.NAME,
				writer.toString(), null);
	}
}
