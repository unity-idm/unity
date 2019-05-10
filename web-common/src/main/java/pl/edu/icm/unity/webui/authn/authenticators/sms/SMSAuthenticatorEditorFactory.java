/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.authenticators.sms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.credential.sms.SMSVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

/**
 * Factory for {@link SMSAuthenticatorEditor}
 * 
 * @author P.Piernik
 *
 */
@Component
class SMSAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private UnityMessageSource msg;
	private CredentialManagement credMan;
	private FileStorageService fileStorageService;
	private URIAccessService uriAccessService;

	@Autowired
	SMSAuthenticatorEditorFactory(UnityMessageSource msg, FileStorageService fileStorageService, URIAccessService uriAccessService,
			CredentialManagement credMan)
	{
		this.msg = msg;
		this.credMan = credMan;
		this.fileStorageService = fileStorageService;
		this.uriAccessService = uriAccessService;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return SMSVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new SMSAuthenticatorEditor(msg, fileStorageService, uriAccessService, credMan.getCredentialDefinitions());
	}
}
