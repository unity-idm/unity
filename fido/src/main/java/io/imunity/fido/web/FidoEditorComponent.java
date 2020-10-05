/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import io.imunity.fido.FidoRegistration;
import io.imunity.fido.component.FidoComponent;
import io.imunity.fido.credential.FidoCredential;
import io.imunity.fido.credential.FidoCredentialInfo;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;
import pl.edu.icm.unity.webui.common.credentials.MissingCredentialException;
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
	private final boolean adminMode;
	private Button addButton;
	private TextField username;
	private Button customizeButton;

	public FidoEditorComponent(final FidoRegistration fidoRegistration, final CredentialEditorContext context, final MessageSource msg)
	{
		this.msg = msg;
		this.adminMode = context.isAdminMode();

		fidoComponent = FidoComponent.builder(msg)
				.fidoRegistration(fidoRegistration)
				.showSuccessNotification(false)
				.entityId(context.getEntityId())
				.credentialName(context.getCredentialName())
				.credentialConfiguration(context.getCredentialConfiguration())
				.newCredentialListener(this::addNewCredential)
				.build();
		fidoComponent.setHeight(1, Unit.PIXELS);

		username = new TextField(msg.getMessage("Fido.username"));
		username.setValue(nonNull(context.getCredentialConfiguration()) ?
				FidoCredential.deserialize(context.getCredentialConfiguration()).getHostName() + " " + msg.getMessage("Fido.defaultUser") : msg.getMessage("Fido.defaultUser"));
		username.setVisible(false);
		username.setWidth(100, Unit.PERCENTAGE);

		credentialsLayout = new VerticalLayout();
		credentialsLayout.setMargin(false);
		credentialsLayout.setSpacing(false);

		customizeButton = new Button(msg.getMessage("Fido.customizeUsername"));
		customizeButton.addStyleName(Styles.vButtonLink.toString());
		customizeButton.addStyleName("u-highlightedLink");
		customizeButton.addClickListener(e -> {
			customizeButton.setVisible(false);
			username.setVisible(true);
		});
		customizeButton.setVisible(isNull(context.getEntityId()));

		addButton = new Button();
		addButton.setDescription(msg.getMessage("Fido.newRegistration"));
		addButton.setCaption(msg.getMessage("Fido.register"));
		addButton.setIcon(Images.fido.getResource());
		addButton.addStyleName(Styles.signInButton.toString());
		addButton.addStyleName("u-passwordSignInButton");
		addButton.setWidth("100%");
		addButton.addClickListener(e -> fidoComponent.invokeRegistration(username.getValue()));

		VerticalLayout root = new VerticalLayout();
		root.setMargin(false);
		root.setSpacing(false);

		root.addComponents(fidoComponent, username, credentialsLayout, addButton, customizeButton);

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
				.filter(info -> info.getState() != FidoCredentialInfoWrapper.CredentialState.DELETED)
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

		addButton.setVisible(nonNull(fidoComponent.getEntityId()) || credentialsLayout.getComponentCount() == 0);
		username.setVisible(username.isVisible() && credentialsLayout.getComponentCount() == 0);
		customizeButton.setVisible(isNull(fidoComponent.getEntityId()) && credentialsLayout.getComponentCount() == 0 && !username.isVisible());
	}

	private void addNewCredential(final FidoCredentialInfo credential)
	{
		FidoCredentialInfo newCredential = credential.copyBuilder()
				.description(msg.getMessage("FidoExc.defaultKeyDesc", credentials.size() + 1))
				.build();
		credentials.add(new FidoCredentialInfoWrapper(FidoCredentialInfoWrapper.CredentialState.NEW, newCredential));
		reload();
	}

	public String getValue() throws IllegalCredentialException
	{
		if (credentials.stream().noneMatch(c -> c.getState() != FidoCredentialInfoWrapper.CredentialState.DELETED))
			throw new MissingCredentialException(msg.getMessage("FidoExc.noKeysToStore"));

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
