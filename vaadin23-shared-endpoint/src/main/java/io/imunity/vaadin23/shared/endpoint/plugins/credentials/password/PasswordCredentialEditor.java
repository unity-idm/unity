/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.credentials.password;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.shared.endpoint.components.ComponentsContainer;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditorContext;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExtraInfo;

import java.util.Optional;

public class PasswordCredentialEditor implements CredentialEditor
{
	private MessageSource msg;
	private NotificationPresenter notificationPresenter;
	private PasswordCredential config;
	private PasswordEditorComponent editor;

	public PasswordCredentialEditor(MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public ComponentsContainer getEditor(CredentialEditorContext context)
	{
		config = new PasswordCredential();
		config.setSerializedConfiguration(JsonUtil.parse(context.getCredentialConfiguration()));
		
		editor = new PasswordEditorComponent(msg, context, config, notificationPresenter);
		
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
		return Optional.of(ret);
	}

	@Override
	public void setCredentialError(EngineException error)
	{
		editor.setCredentialError(error);
	}
}