/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.cert;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.stdext.credential.cert.CertificateVerificator;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;

/**
 * Empty editor and viewer for {@link CertificateVerificator} definitions.
 * @author K. Benedyczak
 */
public class CertificateCredentialDefinitionEditor implements CredentialDefinitionEditor, CredentialDefinitionViewer
{
	private UnityMessageSource msg;

	public CertificateCredentialDefinitionEditor(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public Component getEditor(String credentialDefinitionConfiguration)
	{
		Label label = new Label(msg.getMessage("CertificateCredentialDefinitionEditor.info"));
		FormLayout ret = new CompactFormLayout(label);
		ret.setSpacing(true);
		ret.setMargin(true);
		return ret;
	}

	@Override
	public String getCredentialDefinition() throws IllegalCredentialException
	{
		return "";
	}

	@Override
	public Component getViewer(String credentialDefinitionConfiguration)
	{
		return getEditor(credentialDefinitionConfiguration);
	}
}
