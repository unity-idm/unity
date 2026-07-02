/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import java.text.ParseException;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;

import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;

class ClientPublicKeysCredentialEditor implements CredentialEditor
{
	private final MessageSource msg;
	private TextArea jwksField;

	ClientPublicKeysCredentialEditor(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditor(CredentialEditorContext context)
	{
		jwksField = new TextArea();
		jwksField.setWidthFull();
		jwksField.setHeight("14em");
		jwksField.addClassName(CssClassNames.MONOSPACE.getName());
		jwksField.addClassName(CssClassNames.SMALL_FONT_FIELD.getName());
		jwksField.setPlaceholder(msg.getMessage("ClientPublicKeysCredentialEditor.placeholder"));
		return new ComponentsContainer(jwksField);
	}

	@Override
	public Optional<Component> getViewer(String credentialInfo)
	{
		if (credentialInfo == null || credentialInfo.isBlank())
			return Optional.empty();
		return Optional.of(new Span(msg.getMessage("ClientPublicKeysCredentialEditor.jwksSet")));
	}

	@Override
	public String getValue() throws IllegalCredentialException
	{
		if (jwksField == null)
			return "";
		String value = jwksField.getValue();
		if (value == null || value.isBlank())
			return "";
		try
		{
			com.nimbusds.jose.jwk.JWKSet.parse(value);
		} catch (ParseException e)
		{
			throw new IllegalCredentialException(
					msg.getMessage("ClientPublicKeysCredentialEditor.invalidJwks") + ": " + e.getMessage());
		}
		return value;
	}

	@Override
	public void setCredentialError(EngineException error)
	{
		if (jwksField != null && error != null)
			jwksField.setErrorMessage(error.getMessage());
	}

	@Override
	public boolean isUserConfigurable()
	{
		return false;
	}
}
