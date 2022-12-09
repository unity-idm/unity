/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.credentials.password;

import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.stdext.credential.pass.PasswordEncodingPoolProvider;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;
import pl.edu.icm.unity.webui.common.credentials.pass.PasswordCredentialDefinitionEditor;

@Component
public class PasswordCredentialEditorFactoryV23 implements CredentialEditorFactory
{
	private MessageSource msg;
	private MessageTemplateManagement msgTplMan;
	private PasswordEncodingPoolProvider poolProvider;
	private NotificationPresenter notificationPresenter;
	
	@Autowired
	public PasswordCredentialEditorFactoryV23(MessageSource msg, MessageTemplateManagement msgTplMan,
	                                          PasswordEncodingPoolProvider poolProvider, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.msgTplMan = msgTplMan;
		this.poolProvider = poolProvider;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public String getSupportedCredentialType()
	{
		return PasswordVerificator.NAME;
	}

	@Override
	public CredentialEditor createCredentialEditor()
	{
		return new PasswordCredentialEditor(msg, notificationPresenter);
	}

	@Override
	public CredentialDefinitionEditor creteCredentialDefinitionEditor()
	{
		return new PasswordCredentialDefinitionEditor(msg, msgTplMan, poolProvider);
	}

	@Override
	public CredentialDefinitionViewer creteCredentialDefinitionViewer()
	{
		return new PasswordCredentialDefinitionEditor(msg, msgTplMan, poolProvider);
	}
}
