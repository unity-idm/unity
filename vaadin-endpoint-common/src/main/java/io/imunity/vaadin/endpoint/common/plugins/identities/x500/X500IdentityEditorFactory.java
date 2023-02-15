/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.identities.x500;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditor;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.stdext.identity.X500Identity;

@Component
public class X500IdentityEditorFactory implements IdentityEditorFactory
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	@Autowired
	public X500IdentityEditorFactory(MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public String getSupportedIdentityType()
	{
		return X500Identity.ID;
	}

	@Override
	public IdentityEditor createInstance()
	{
		return new X500IdentityEditor(msg, notificationPresenter);
	}
}
