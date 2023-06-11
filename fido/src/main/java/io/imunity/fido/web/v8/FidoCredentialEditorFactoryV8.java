/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web.v8;

import io.imunity.fido.FidoRegistration;
import io.imunity.fido.service.FidoCredentialVerificator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageSource;
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
public class FidoCredentialEditorFactoryV8 implements CredentialEditorFactory
{
	private MessageSource msg;
	private FidoRegistration fidoRegistration;

	@Autowired
	public FidoCredentialEditorFactoryV8(final MessageSource msg, final FidoRegistration fidoRegistration)
	{
		this.msg = msg;
		this.fidoRegistration = fidoRegistration;
	}

	@Override
	public String getSupportedCredentialType()
	{
		return FidoCredentialVerificator.NAME;
	}

	@Override
	public CredentialEditor createCredentialEditor()
	{
		return new FidoCredentialEditor(msg, fidoRegistration);
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
