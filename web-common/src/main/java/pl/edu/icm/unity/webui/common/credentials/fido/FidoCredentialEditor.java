/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.fido;

import pl.edu.icm.unity.engine.api.FidoManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
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
public class FidoCredentialEditor implements CredentialEditor
{
	private FidoManagement fidoService;
	private FidoEditorComponent editorComponent;
	private UnityMessageSource msg;

	public FidoCredentialEditor(final UnityMessageSource msg, final FidoManagement fidoService)
	{
		this.msg = msg;
		this.fidoService = fidoService;
	}

	@Override
	public ComponentsContainer getEditor(CredentialEditorContext context)
	{
		if (isNull(editorComponent))
			editorComponent = new FidoEditorComponent(fidoService, context, msg);

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
