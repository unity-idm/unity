/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.authenticators.cert;

import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.stdext.credential.cert.CertificateVerificator;


@Component
class CertificateAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private final MessageSource msg;
	private final CredentialManagement credMan;

	CertificateAuthenticatorEditorFactory(MessageSource msg, CredentialManagement credMan)
	{
		this.msg = msg;
		this.credMan = credMan;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return CertificateVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new CertificateAuthenticatorEditor(msg, credMan.getCredentialDefinitions());
	}
}
