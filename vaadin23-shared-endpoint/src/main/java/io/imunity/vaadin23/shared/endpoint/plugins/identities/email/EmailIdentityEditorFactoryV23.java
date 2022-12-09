/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.identities.email;

import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.shared.endpoint.plugins.identities.IdentityEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.identities.IdentityEditorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;


@Component
public class EmailIdentityEditorFactoryV23 implements IdentityEditorFactory
{
	private final MessageSource msg;
	private final EmailConfirmationManager emailConfirmationMan;
	private final EntityResolver idResolver;
	private final ConfirmationInfoFormatter formatter;
	private final NotificationPresenter notificationPresenter;

	@Autowired
	public EmailIdentityEditorFactoryV23(MessageSource msg,
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
