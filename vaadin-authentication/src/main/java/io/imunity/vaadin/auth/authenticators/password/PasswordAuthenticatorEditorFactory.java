/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.vaadin.auth.authenticators.password;

import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;

/**
 * Factory for {@link PasswordAuthenticatorEditor}
 * @author P.Piernik
 *
 */
@Component
class PasswordAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private MessageSource msg;
	private CredentialManagement credMan;
	
	@Autowired
	PasswordAuthenticatorEditorFactory(MessageSource msg, CredentialManagement credMan)
	{
		this.msg = msg;
		this.credMan = credMan;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return PasswordVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		
		
		return new PasswordAuthenticatorEditor(msg, credMan.getCredentialDefinitions());
	}

}
