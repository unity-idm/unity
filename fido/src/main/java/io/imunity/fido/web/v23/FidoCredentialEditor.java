/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web.v23;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.fido.FidoRegistration;
import io.imunity.fido.credential.FidoCredentialInfo;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditorContext;
import io.imunity.vaadin23.shared.endpoint.components.ComponentsContainer;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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

		ret.add(new Label(msg.getMessage("Fido.viewerInfo",
				keys.size())));
		return Optional.of(ret);
	}

	@Override
	public void setCredentialError(EngineException exception)
	{
		editorComponent.setCredentialError(exception);
	}
}