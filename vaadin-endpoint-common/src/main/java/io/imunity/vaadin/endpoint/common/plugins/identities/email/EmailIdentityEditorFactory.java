/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.identities.email;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.ConfirmationInfoFormatter;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditor;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;


@Component
public class EmailIdentityEditorFactory implements IdentityEditorFactory
{
	private final MessageSource msg;
	private final EmailConfirmationManager emailConfirmationMan;
	private final EntityResolver idResolver;
	private final ConfirmationInfoFormatter formatter;
	private final NotificationPresenter notificationPresenter;

	@Autowired
	public EmailIdentityEditorFactory(MessageSource msg,
	                                  EmailConfirmationManager emailConfirmationMan, EntityResolver idResolver,
	                                  ConfirmationInfoFormatter formatter, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.emailConfirmationMan = emailConfirmationMan;
		this.idResolver = idResolver;
		this.formatter = formatter;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public String getSupportedIdentityType()
	{
		return EmailIdentity.ID;
	}

	@Override
	public IdentityEditor createInstance()
	{
		return new EmailIdentityEditor(msg, emailConfirmationMan, idResolver, formatter, notificationPresenter);
	}
}
