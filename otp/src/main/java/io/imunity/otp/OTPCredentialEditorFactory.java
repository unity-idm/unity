/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorFactory;

@Component
class OTPCredentialEditorFactory implements CredentialEditorFactory
{
	private final ObjectFactory<OTPCredentialDefinitionEditor> credDefEditorFactory;
	private final ObjectFactory<CredentialDefinitionViewer> credDefViewerFactory;
	private final ObjectFactory<CredentialEditor> credentialEditiorFactory;

	@Autowired
	public OTPCredentialEditorFactory(
			ObjectFactory<OTPCredentialDefinitionEditor> credDefEditorFactory,
			ObjectFactory<CredentialDefinitionViewer> credDefViewerFactory,
			ObjectFactory<CredentialEditor> credentialEditiorFactory)
	{
		this.credDefEditorFactory = credDefEditorFactory;
		this.credDefViewerFactory = credDefViewerFactory;
		this.credentialEditiorFactory = credentialEditiorFactory;
	}

	@Override
	public String getSupportedCredentialType()
	{
		return OTP.NAME;
	}

	@Override
	public CredentialEditor createCredentialEditor()
	{
		return credentialEditiorFactory.getObject();
	}

	@Override
	public CredentialDefinitionEditor creteCredentialDefinitionEditor()
	{
		return credDefEditorFactory.getObject();
	}

	@Override
	public CredentialDefinitionViewer creteCredentialDefinitionViewer()
	{
		return credDefViewerFactory.getObject();
	}
}
