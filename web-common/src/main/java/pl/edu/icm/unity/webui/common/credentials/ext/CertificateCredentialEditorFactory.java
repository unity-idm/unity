/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.ext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.CertificateVerificatorFactory;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorFactory;

@Component
public class CertificateCredentialEditorFactory implements CredentialEditorFactory
{
	private UnityMessageSource msg;
	
	@Autowired
	public CertificateCredentialEditorFactory(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedCredentialType()
	{
		return CertificateVerificatorFactory.NAME;
	}

	@Override
	public CredentialEditor createInstance()
	{
		return new CertificateCredentialEditor(msg);
	}

}
