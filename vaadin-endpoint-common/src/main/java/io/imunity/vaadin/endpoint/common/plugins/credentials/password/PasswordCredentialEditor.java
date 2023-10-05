/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials.password;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExtraInfo;

import java.util.Optional;

public class PasswordCredentialEditor implements CredentialEditor
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
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
		ret.setPadding(false);
		PasswordExtraInfo pei = PasswordExtraInfo.fromJson(credentialExtraInformation);
		if (pei.getLastChange() == null)
			return Optional.empty();
		
		ret.add(new Span(msg.getMessage("PasswordCredentialEditor.lastModification",
				pei.getLastChange())));
		
		PasswordCredentialResetSettings resetS = config.getPasswordResetSettings();
		if (resetS.isEnabled() && !resetS.getQuestions().isEmpty())
		{
			String secQ = pei.getSecurityQuestion() == null ? 
					msg.getMessage("PasswordCredentialEditor.notDefined")
					: pei.getSecurityQuestion();
			ret.add(new Span(msg.getMessage("PasswordCredentialEditor.securityQuestion", secQ)));
		}
		return Optional.of(ret);
	}

	@Override
	public void setCredentialError(EngineException error)
	{
		editor.setCredentialError(error);
	}
}
