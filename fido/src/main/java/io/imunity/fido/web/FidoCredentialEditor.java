/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web;

import io.imunity.fido.FidoRegistration;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Allows to edit single Fido credential (with multiple keys).
 *
 * @author R. Ledzinski
 */
class FidoCredentialEditor implements CredentialEditor
{
	private FidoRegistration fidoRegistration;
	private FidoEditorComponent editorComponent;
	private MessageSource msg;

	public FidoCredentialEditor(final MessageSource msg, final FidoRegistration fidoRegistration)
	{
		this.msg = msg;
		this.fidoRegistration = fidoRegistration;
	}

	@Override
	public ComponentsContainer getEditor(CredentialEditorContext context)
	{
		if (isNull(editorComponent))
			editorComponent = new FidoEditorComponent(fidoRegistration, context, msg);

		return new ComponentsContainer(editorComponent);
	}

	@Override
	public String getValue() throws IllegalCredentialException
	{
		return editorComponent.getValue();
	}

	@Override
	public ComponentsContainer getViewer(String credentialConfiguration)
	{
		// Viewer is empty - editor handles both functions.
		// Make sure editor is reloaded when needed.
		if (nonNull(editorComponent))
		{
			editorComponent.initUI(credentialConfiguration);
		}
		return new ComponentsContainer();
	}

	@Override
	public void setCredentialError(EngineException exception)
	{
		editorComponent.setCredentialError(exception);
	}
}
