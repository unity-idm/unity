/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web;

import io.imunity.fido.FidoRegistration;
import io.imunity.fido.service.FidoCredentialVerificator;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionViewer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorFactory;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;

@Component
class FidoCredentialEditorFactory implements CredentialEditorFactory
{
	private final MessageSource msg;
	private final HtmlTooltipFactory htmlTooltipFactory;
	private final FidoRegistration fidoRegistration;
	private final NotificationPresenter notificationPresenter;

	FidoCredentialEditorFactory(MessageSource msg, FidoRegistration fidoRegistration,
			HtmlTooltipFactory htmlTooltipFactory, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.htmlTooltipFactory = htmlTooltipFactory;
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
		return new FidoCredentialDefinitionEditor(msg, htmlTooltipFactory);
	}

	@Override
	public CredentialDefinitionViewer creteCredentialDefinitionViewer()
	{
		return new FidoCredentialDefinitionEditor(msg, htmlTooltipFactory);
	}
}
