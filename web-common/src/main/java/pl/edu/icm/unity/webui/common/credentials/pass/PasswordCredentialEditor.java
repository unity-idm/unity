/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.pass;

import com.vaadin.ui.Label;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExtraInfo;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;

/**
 * Allows to setup password for password credential.
 * @author K. Benedyczak
 */
public class PasswordCredentialEditor implements CredentialEditor
{
	private UnityMessageSource msg;
	private PasswordCredential config;
	private PasswordEditorComponent editor;

	public PasswordCredentialEditor(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditor(CredentialEditorContext context)
	{
		config = new PasswordCredential();
		config.setSerializedConfiguration(JsonUtil.parse(context.getCredentialConfiguration()));
		
		editor = new PasswordEditorComponent(msg, context, config);
		
		return new ComponentsContainer(editor);
	}

	@Override
	public String getValue() throws IllegalCredentialException
	{
		return editor.getValue();
	}

	@Override
	public ComponentsContainer getViewer(String credentialExtraInformation)
	{
		ComponentsContainer ret = new ComponentsContainer();
		PasswordExtraInfo pei = PasswordExtraInfo.fromJson(credentialExtraInformation);
		if (pei.getLastChange() == null)
			return ret;
		
		ret.add(new Label(msg.getMessage("PasswordCredentialEditor.lastModification", 
				pei.getLastChange())));
		
		PasswordCredentialResetSettings resetS = config.getPasswordResetSettings();
		if (resetS.isEnabled() && !resetS.getQuestions().isEmpty())
		{
			String secQ = pei.getSecurityQuestion() == null ? 
					msg.getMessage("PasswordCredentialEditor.notDefined")
					: pei.getSecurityQuestion();
			ret.add(new Label(msg.getMessage("PasswordCredentialEditor.securityQuestion", secQ)));
		}
		return ret;
	}

	@Override
	public void setCredentialError(EngineException error)
	{
		editor.setCredentialError(error);
	}
}
