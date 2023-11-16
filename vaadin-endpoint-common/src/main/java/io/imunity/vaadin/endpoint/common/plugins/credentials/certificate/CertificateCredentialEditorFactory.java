/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials.certificate;

import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionViewer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorFactory;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.stdext.credential.cert.CertificateVerificator;

@Component
class CertificateCredentialEditorFactory implements CredentialEditorFactory
{
	private final MessageSource msg;
	
	CertificateCredentialEditorFactory(MessageSource msg)
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
