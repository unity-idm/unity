/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp.v23;

import io.imunity.otp.OTP;
import io.imunity.otp.OTPCredentialDefinitionEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditorFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;


@Component
class OTPCredentialEditorFactoryV23 implements CredentialEditorFactory
{
	private final ObjectFactory<OTPCredentialDefinitionEditor> credDefEditorFactory;
	private final ObjectFactory<CredentialDefinitionViewer> credDefViewerFactory;
	private final ObjectFactory<CredentialEditor> credentialEditiorFactory;

	@Autowired
	public OTPCredentialEditorFactoryV23(
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
