/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web.v8;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import io.imunity.fido.FidoRegistration;
import io.imunity.fido.credential.FidoCredentialInfo;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;

import java.util.List;
import java.util.Optional;

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
	public boolean isCredentialCleared()
	{
		return editorComponent.getNonDeletedKeysNumber() == 0;
	}

	@Override
	public Optional<Component> getViewer(String credentialInfo)
	{
		if (isNull(credentialInfo))
		{
			return Optional.empty();
		}
		if (nonNull(editorComponent))
		{
			editorComponent.initUI(credentialInfo);
		}
		List<FidoCredentialInfo> keys = FidoCredentialInfo.deserializeList(credentialInfo);
		if (keys.size() == 0)
		{
			return Optional.empty();
		}
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);

		ret.addComponent(new Label(msg.getMessage("Fido.viewerInfo",
				keys.size())));
		return Optional.of(ret);
	}

	@Override
	public void setCredentialError(EngineException exception)
	{
		editorComponent.setCredentialError(exception);
	}
}
