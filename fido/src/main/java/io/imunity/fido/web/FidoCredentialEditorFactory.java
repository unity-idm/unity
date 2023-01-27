/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web;

import io.imunity.fido.FidoRegistration;
import io.imunity.fido.service.FidoCredentialVerificator;
import io.imunity.fido.web.v8.FidoCredentialDefinitionEditor;
import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin23.endpoint.common.plugins.credentials.CredentialEditorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;

@Component
class FidoCredentialEditorFactory implements CredentialEditorFactory
{
	private MessageSource msg;
	private FidoRegistration fidoRegistration;
	private NotificationPresenter notificationPresenter;

	@Autowired
	public FidoCredentialEditorFactory(final MessageSource msg, final FidoRegistration fidoRegistration,
	                                   NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.fidoRegistration = fidoRegistration;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public String getSupportedCredentialType()
	{
		return FidoCredentialVerificator.NAME;
	}

	@Override
	public CredentialEditor createCredentialEditor()
	{
		return new FidoCredentialEditor(msg, fidoRegistration, notificationPresenter);
	}

	@Override
	public CredentialDefinitionEditor creteCredentialDefinitionEditor()
	{
		return new FidoCredentialDefinitionEditor(msg);
	}

	@Override
	public CredentialDefinitionViewer creteCredentialDefinitionViewer()
	{
		return new FidoCredentialDefinitionEditor(msg);
	}
}
