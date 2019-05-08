/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.authenticators.cert;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.credential.cert.CertificateVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

/**
 * Factory for {@link CertificateAuthenticatorEditor}
 * 
 * @author P.Piernik
 *
 */
@Component
class CertificateAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private UnityMessageSource msg;
	private CredentialManagement credMan;
	private FileStorageService fileStorageService;
	
	CertificateAuthenticatorEditorFactory(UnityMessageSource msg, FileStorageService fileStorageService,
			CredentialManagement credMan)
	{
		this.msg = msg;
		this.credMan = credMan;
		this.fileStorageService = fileStorageService;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return CertificateVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new CertificateAuthenticatorEditor(msg, fileStorageService, credMan.getCredentialDefinitions());
	}
}
