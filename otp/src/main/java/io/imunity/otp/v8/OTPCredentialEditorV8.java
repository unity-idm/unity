/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp.v8;

import com.google.common.base.Strings;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import io.imunity.otp.OTPCredentialDefinition;
import io.imunity.otp.OTPExtraInfo;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;

import java.util.Optional;

@PrototypeComponent
class OTPCredentialEditorV8 implements CredentialEditor
{
	private MessageSource msg;
	private OTPEditorComponent editor;
	private OTPCredentialDefinition config;
	
	@Autowired
	OTPCredentialEditorV8(MessageSource msg)
	{
		this.msg = msg;
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
			NotificationPopup.showError(msg, 
				msg.getMessage("CredentialChangeDialog.credentialUpdateError"), 
				error);
	}
}
