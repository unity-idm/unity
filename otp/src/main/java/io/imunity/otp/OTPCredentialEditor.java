/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp;

import com.google.common.base.Strings;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import java.util.Optional;

@PrototypeComponent
class OTPCredentialEditor implements CredentialEditor
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private OTPEditorComponent editor;

	@Autowired
	OTPCredentialEditor(MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public ComponentsContainer getEditor(CredentialEditorContext context)
	{
		OTPCredentialDefinition config = JsonUtil.parse(context.getCredentialConfiguration(),
				OTPCredentialDefinition.class); 
		editor = new OTPEditorComponent(msg, context, config);
		return new ComponentsContainer(editor);
	}

	@Override
	public Optional<Component> getViewer(String credentialInfo)
	{
		if (Strings.isNullOrEmpty(credentialInfo))
			return Optional.empty();
		OTPExtraInfo extraInfo = JsonUtil.parse(credentialInfo, OTPExtraInfo.class);
		Span lastChange = new Span(msg.getMessage("OTPCredentialEditor.lastModification",
				extraInfo.lastModification));
		return Optional.of(lastChange);
	}
	
	@Override
	public String getValue() throws IllegalCredentialException
	{
		return editor.getValue();
	}

	@Override
	public void setCredentialError(EngineException error)
	{
		if (error != null)
			notificationPresenter.showError(
				msg.getMessage("CredentialChangeDialog.credentialUpdateError"), 
				error.getMessage());
	}
}
