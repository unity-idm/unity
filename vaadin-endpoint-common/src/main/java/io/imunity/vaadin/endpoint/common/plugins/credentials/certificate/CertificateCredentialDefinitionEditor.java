/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials.certificate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionViewer;
import pl.edu.icm.unity.base.message.MessageSource;

class CertificateCredentialDefinitionEditor implements CredentialDefinitionEditor, CredentialDefinitionViewer
{
	private final MessageSource msg;

	public CertificateCredentialDefinitionEditor(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public Component getEditor(String credentialDefinitionConfiguration)
	{
		return new VerticalLayout(new Span(msg.getMessage("CertificateCredentialDefinitionEditor.info")));
	}

	@Override
	public String getCredentialDefinition()
	{
		return "";
	}

	@Override
	public Component getViewer(String credentialDefinitionConfiguration)
	{
		return getEditor(credentialDefinitionConfiguration);
	}
}
