/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.composite.password.web;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.composite.password.CompositePasswordVerificator;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.pam.web.PamAuthenticatorEditorFactory;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

/**
 * Factory for {@link CompositePasswordAuthenticatorEditor}
 * @author P.Piernik
 *
 */
@Component
public class CompositePasswordAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private UnityMessageSource msg;
	private CredentialManagement credMan;
	private PamAuthenticatorEditorFactory pamFactory;
	
	CompositePasswordAuthenticatorEditorFactory(UnityMessageSource msg, CredentialManagement credMan, PamAuthenticatorEditorFactory pamFactory)
	{
		this.msg = msg;
		this.credMan = credMan;
		this.pamFactory = pamFactory;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return CompositePasswordVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new CompositePasswordAuthenticatorEditor(msg, credMan.getCredentialDefinitions(), pamFactory);
	}

}
