/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.pass;

import java.util.Optional;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
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
	private MessageSource msg;
	private PasswordCredential config;
	private PasswordEditorComponent editor;

	public PasswordCredentialEditor(MessageSource msg)
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
	public Optional<Component> getViewer(String credentialExtraInformation)
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		PasswordExtraInfo pei = PasswordExtraInfo.fromJson(credentialExtraInformation);
		if (pei.getLastChange() == null)
			return Optional.empty();
		
		ret.addComponent(new Label(msg.getMessage("PasswordCredentialEditor.lastModification", 
				pei.getLastChange())));
		
		PasswordCredentialResetSettings resetS = config.getPasswordResetSettings();
		if (resetS.isEnabled() && !resetS.getQuestions().isEmpty())
		{
			String secQ = pei.getSecurityQuestion() == null ? 
					msg.getMessage("PasswordCredentialEditor.notDefined")
					: pei.getSecurityQuestion();
			ret.addComponent(new Label(msg.getMessage("PasswordCredentialEditor.securityQuestion", secQ)));
		}
		return Optional.of(ret);
	}

	@Override
	public void setCredentialError(EngineException error)
	{
		editor.setCredentialError(error);
	}
}
