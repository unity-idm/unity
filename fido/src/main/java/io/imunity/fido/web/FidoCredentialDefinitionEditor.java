/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;

/**
 * Basic Fido credential definition editor. Requires configuration to be added.
 *
 * @author R. Ledzinski
 */
class FidoCredentialDefinitionEditor implements CredentialDefinitionEditor, CredentialDefinitionViewer
{
	private MessageSource msg;

	public FidoCredentialDefinitionEditor(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public Component getEditor(String credentialDefinitionConfiguration)
	{
		Label label = new Label("Not used for now"); // FIXME
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
