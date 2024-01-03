/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.composite.password.web.v8;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.composite.password.CompositePasswordVerificator;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.ldap.client.console.v8.LdapAuthenticatorEditorFactory;
import pl.edu.icm.unity.pam.web.v8.PamAuthenticatorEditorFactory;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

/**
 * Factory for {@link CompositePasswordAuthenticatorEditor}
 * 
 * @author P.Piernik
 *
 */
@Component("CompositePasswordAuthenticatorEditorFactoryV8")
class CompositePasswordAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private MessageSource msg;
	private CredentialManagement credMan;
	private PamAuthenticatorEditorFactory pamFactory;
	private LdapAuthenticatorEditorFactory ldapFactory;

	CompositePasswordAuthenticatorEditorFactory(MessageSource msg, CredentialManagement credMan,
			PamAuthenticatorEditorFactory pamFactory, LdapAuthenticatorEditorFactory ldapFactory)
	{
		this.msg = msg;
		this.credMan = credMan;
		this.pamFactory = pamFactory;
		this.ldapFactory = ldapFactory;
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
				ldapFactory);
	}
}
