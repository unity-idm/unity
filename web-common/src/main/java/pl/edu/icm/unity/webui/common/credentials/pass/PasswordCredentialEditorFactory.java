/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.pass;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorFactory;

@Component
public class PasswordCredentialEditorFactory implements CredentialEditorFactory
{
	private UnityMessageSource msg;
	private MessageTemplateManagement msgTplMan;
	
	@Autowired
	public PasswordCredentialEditorFactory(UnityMessageSource msg, MessageTemplateManagement msgTplMan)
	{
		this.msg = msg;
		this.msgTplMan = msgTplMan;
	}

	@Override
	public String getSupportedCredentialType()
	{
		return PasswordVerificator.NAME;
	}

	@Override
	public CredentialEditor createCredentialEditor()
	{
		return new PasswordCredentialEditor(msg);
	}

	@Override
	public CredentialDefinitionEditor creteCredentialDefinitionEditor()
	{
		return new PasswordCredentialDefinitionEditor(msg, msgTplMan);
	}

	@Override
	public CredentialDefinitionViewer creteCredentialDefinitionViewer()
	{
		return new PasswordCredentialDefinitionEditor(msg, msgTplMan);
	}
}
