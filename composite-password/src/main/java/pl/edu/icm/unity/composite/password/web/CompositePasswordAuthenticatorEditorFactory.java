/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.composite.password.web;

import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import io.imunity.vaadin.elements.NotificationPresenter;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.composite.password.CompositePasswordVerificator;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.ldap.client.console.LdapAuthenticatorEditorFactory;
import pl.edu.icm.unity.pam.web.PamAuthenticatorEditorFactory;


@Component
class CompositePasswordAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private final MessageSource msg;
	private final CredentialManagement credMan;
	private final PamAuthenticatorEditorFactory pamFactory;
	private final LdapAuthenticatorEditorFactory ldapFactory;
	private final NotificationPresenter notificationPresenter;

	CompositePasswordAuthenticatorEditorFactory(MessageSource msg, CredentialManagement credMan,
			PamAuthenticatorEditorFactory pamFactory, LdapAuthenticatorEditorFactory ldapFactory,
			NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.credMan = credMan;
		this.pamFactory = pamFactory;
		this.ldapFactory = ldapFactory;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return CompositePasswordVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new CompositePasswordAuthenticatorEditor(msg, credMan.getCredentialDefinitions(), pamFactory,
				ldapFactory, notificationPresenter);
	}
}
