/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web;

import io.imunity.fido.FidoManagement;
import io.imunity.fido.credential.FidoCredentialVerificator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorFactory;

/**
 * Factory for {@link FidoCredentialEditor}
 *
 * @author R. Ledznski
 */
@Component
public class FidoCredentialEditorFactory implements CredentialEditorFactory
{
	private MessageSource msg;
	private FidoManagement fidoService;

	@Autowired
	public FidoCredentialEditorFactory(final MessageSource msg, final FidoManagement fidoService)
	{
		this.msg = msg;
		this.fidoService = fidoService;
	}

	@Override
	public String getSupportedCredentialType()
	{
		return FidoCredentialVerificator.NAME;
	}

	@Override
	public CredentialEditor createCredentialEditor()
	{
		return new FidoCredentialEditor(msg, fidoService);
	}

	@Override
	public CredentialDefinitionEditor creteCredentialDefinitionEditor()
	{
		return new FidoCredentialDefinitionEditor(msg);
	}

	@Override
	public CredentialDefinitionViewer creteCredentialDefinitionViewer()
	{
		return new FidoCredentialDefinitionEditor(msg);
	}
}
