/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.credentials.certificate;

import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.stdext.credential.cert.CertificateVerificator;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;
import pl.edu.icm.unity.webui.common.credentials.cert.CertificateCredentialDefinitionEditor;

@Component
public class CertificateCredentialEditorFactoryV23 implements CredentialEditorFactory
{
	private MessageSource msg;
	
	@Autowired
	public CertificateCredentialEditorFactoryV23(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedCredentialType()
	{
		return CertificateVerificator.NAME;
	}

	@Override
	public CredentialEditor createCredentialEditor()
	{
		return new CertificateCredentialEditor();
	}

	@Override
	public CredentialDefinitionEditor creteCredentialDefinitionEditor()
	{
		return new CertificateCredentialDefinitionEditor(msg);
	}

	@Override
	public CredentialDefinitionViewer creteCredentialDefinitionViewer()
	{
		return new CertificateCredentialDefinitionEditor(msg);
	}
}
