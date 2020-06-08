/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;
import io.imunity.fido.FidoManagement;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import io.imunity.fido.component.FidoComponent;
import io.imunity.fido.credential.FidoCredentialInfo;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Editor integrating FidoComponent and displays current Fido keys with status.
 *
 * @author R. Ledzinski
 */
class FidoEditorComponent extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, FidoEditorComponent.class);

	private MessageSource msg;
	private final List<FidoCredentialInfoWrapper> credentials = new ArrayList<>();
	private final FidoComponent fidoComponent;
	private final VerticalLayout credentialsLayout;

	public FidoEditorComponent(final FidoManagement fidoService, final CredentialEditorContext context, final MessageSource msg)
	{
		this.msg = msg;

		fidoComponent = FidoComponent.builder(fidoService ,msg)
				.showSuccessNotification(false)
				.entityId(context.getEntityId())
				.newCredentialListener(this::addNewCredential)
				.build();

		Button addButton = new Button();
		addButton.setDescription(msg.getMessage("Fido.newRegistration"));
		addButton.setIcon(Images.add.getResource());
		addButton.addStyleName(Styles.vButtonLink.toString());
		addButton.addStyleName(Styles.toolbarButton.toString());
		addButton.addClickListener(e -> fidoComponent.invokeRegistration());

		VerticalLayout root = new VerticalLayout();
		root.setMargin(false);
		root.setSpacing(false);

		credentialsLayout = new VerticalLayout();
		credentialsLayout.setMargin(false);
		credentialsLayout.setSpacing(false);

		root.addComponents(fidoComponent, addButton, credentialsLayout);

		setCompositionRoot(root);

		initUI(context.getExtraInformation());
	}

	void initUI(final String extraInformation)
	{
		initCredentials(extraInformation);
		reload();
	}

	private void initCredentials(String extraInformation)
	{
		credentials.clear();

		if (isNull(extraInformation) || extraInformation.isEmpty())
		{
			return;
		}

		credentials.addAll(FidoCredentialInfo.deserializeList(extraInformation).stream()
				.map(info -> new FidoCredentialInfoWrapper(FidoCredentialInfoWrapper.CredentialState.STORED, info))
				.collect(Collectors.toList()));
	}

	private void reload()
	{
		credentialsLayout.removeAllComponents();

		credentials.stream()
				.map(info -> (Component) new FidoPreviewComponent(msg, info, this::reload))
				.forEach(comp ->
				{
					credentialsLayout.addComponent(HtmlTag.horizontalLine());
					credentialsLayout.addComponent(comp);
				});

		if (credentialsLayout.getComponentCount() > 0)
		{
			credentialsLayout.addComponent(HtmlTag.horizontalLine());
		}
	}

	private void addNewCredential(final FidoCredentialInfo credential)
	{
		credentials.add(new FidoCredentialInfoWrapper(FidoCredentialInfoWrapper.CredentialState.NEW, credential));
		reload();
	}

	public String getValue() throws IllegalCredentialException
	{
		return FidoCredentialInfo.serializeList(credentials.stream()
				.filter(info -> info.getState() != FidoCredentialInfoWrapper.CredentialState.DELETED)
				.map(FidoCredentialInfoWrapper::getCredential)
				.collect(Collectors.toList()));
	}

	public void setCredentialError(EngineException error)
	{
		if (nonNull(error))
			fidoComponent.showError("Error", error.getLocalizedMessage());
	}
}
