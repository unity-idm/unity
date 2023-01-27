/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp;

import com.google.common.base.Strings;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Label;
import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin23.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin23.endpoint.common.plugins.credentials.CredentialEditorContext;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;

import java.util.Optional;

@PrototypeComponent
class OTPCredentialEditor implements CredentialEditor
{
	private MessageSource msg;
	private OTPEditorComponent editor;
	private OTPCredentialDefinition config;
	private NotificationPresenter notificationPresenter;

	@Autowired
	OTPCredentialEditor(MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public ComponentsContainer getEditor(CredentialEditorContext context)
	{
		config = JsonUtil.parse(context.getCredentialConfiguration(), 
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
		Label lastChange = new Label(msg.getMessage("OTPCredentialEditor.lastModification",
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
