/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;

@PrototypeComponent
class OTPCredentialEditor implements CredentialEditor
{
	private MessageSource msg;
	private OTPEditorComponent editor;
	private OTPCredentialDefinition config;
	
	@Autowired
	OTPCredentialEditor(MessageSource msg)
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
	public ComponentsContainer getViewer(String credentialInfo)
	{
		return new ComponentsContainer(new OTPVieverComponent());
	}
	
	@Override
	public String getValue() throws IllegalCredentialException
	{
		return editor.getValue();
	}

	@Override
	public void setCredentialError(EngineException error)
	{
		editor.setCredentialError(error);
	}
}
