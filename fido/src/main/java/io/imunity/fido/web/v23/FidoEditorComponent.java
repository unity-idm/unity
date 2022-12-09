/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web.v23;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinService;
import io.imunity.fido.FidoRegistration;
import io.imunity.fido.credential.FidoCredential;
import io.imunity.fido.credential.FidoCredentialInfo;
import io.imunity.fido.web.FidoCredentialInfoWrapper;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditorContext;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.credentials.MissingCredentialException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.Unit.PIXELS;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

class FidoEditorComponent extends HorizontalLayout
{
	private MessageSource msg;
	private final List<FidoCredentialInfoWrapper> credentials = new ArrayList<>();
	private final FidoComponent fidoComponent;
	private final VerticalLayout credentialsLayout;
	private Button addButton;
	private TextField username;
	private Button advancedOptionsButton;
	private boolean loginLessSupported;
	private VerticalLayout buttons;
	private VerticalLayout advancedOptions;
	private Checkbox loginLessAllowed;

	public FidoEditorComponent(FidoRegistration fidoRegistration, CredentialEditorContext context,
	                           MessageSource msg)
	{
		this.msg = msg;

		fidoComponent = FidoComponent.builder(msg)
				.fidoRegistration(fidoRegistration)
				.showSuccessNotification(false)
				.entityId(context.getEntityId())
				.credentialName(context.getCredentialName())
				.credentialConfiguration(context.getCredentialConfiguration())
				.newCredentialListener(this::addNewCredential)
				.allowAuthenticatorReUsage(isInDevelopmentMode())
				.build();
		fidoComponent.setHeight(0, PIXELS);

		Optional<FidoCredential> credential = Optional.ofNullable(context.getCredentialConfiguration())
				.map(s -> FidoCredential.deserialize(context.getCredentialConfiguration()));
		loginLessSupported = credential.map(FidoCredential::isLoginLessAllowed).orElse(false);

		username = new TextField(msg.getMessage("Fido.username"));
		username.setValue(credential.map(c -> c.getHostName() + " " + msg.getMessage("Fido.defaultUser")).orElse(msg.getMessage("Fido.defaultUser")));
		username.setWidth(100, Unit.PERCENTAGE);

		credentialsLayout = new VerticalLayout();
		credentialsLayout.setMargin(false);
		credentialsLayout.setSpacing(false);

		addButton = new Button();
		addButton.getElement().setProperty("title", msg.getMessage("Fido.newRegistration"));
		addButton.setText(msg.getMessage("Fido.register"));
		addButton.setWidth("100%");
		addButton.addClickListener(e -> fidoComponent.invokeRegistration(username.getValue(), loginLessAllowed.getValue()));

		advancedOptionsButton = new Button(msg.getMessage("Fido.advancedOptions"));
		advancedOptionsButton.addClickListener(e -> {
			advancedOptions.setVisible(!advancedOptions.isVisible());
			reloadAdvancedOptions();
		});

		loginLessAllowed = new Checkbox(msg.getMessage("Fido.credEditor.loginLess"));
		loginLessAllowed.getElement().setProperty("title", msg.getMessage("Fido.credEditor.loginLess.tip"));
		loginLessAllowed.setValue(loginLessSupported);

		buttons = new VerticalLayout(addButton, advancedOptionsButton);
		buttons.setSpacing(false);
		buttons.setMargin(false);

		advancedOptions = new VerticalLayout(username, loginLessAllowed);
		advancedOptions.setMargin(false);
		advancedOptions.setVisible(false);

		VerticalLayout root = new VerticalLayout();
		root.setMargin(false);
		root.setSpacing(true);
		root.addClassName("u-fidoEditorLayout");
		
		root.add(fidoComponent, credentialsLayout, buttons, advancedOptions);

		initUI(context.getExtraInformation());
	}

	private boolean isInDevelopmentMode()
	{
		VaadinService vaadinService = VaadinService.getCurrent();
		if (vaadinService == null)
			return false;
		return !vaadinService.getDeploymentConfiguration().isProductionMode();
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
		credentialsLayout.removeAll();

		credentials.stream()
				.filter(info -> info.getState() != FidoCredentialInfoWrapper.CredentialState.DELETED)
				.map(info -> (Component) new FidoPreviewComponent(msg, info, this::reload))
				.forEach(comp ->
				{
					credentialsLayout.add(new Html("<br>"));
					credentialsLayout.add(comp);
				});

		if (credentialsLayout.getComponentCount() > 0)
		{
			credentialsLayout.setVisible(true);
			credentialsLayout.add(new Html("<br>"));
		} else
		{
			credentialsLayout.setVisible(false);
		}

		addButton.setVisible(nonNull(fidoComponent.getEntityId()) || credentialsLayout.getComponentCount() == 0);
		reloadAdvancedOptions();
	}

	private void reloadAdvancedOptions()
	{
		username.setVisible(isNull(fidoComponent.getEntityId()));
		loginLessAllowed.setVisible(loginLessSupported);

		if (!username.isVisible() && !loginLessAllowed.isVisible()) {
			advancedOptions.setVisible(false);
			advancedOptionsButton.setVisible(false);
			return;
		}

		advancedOptionsButton.setVisible(nonNull(fidoComponent.getEntityId()) || credentialsLayout.getComponentCount() == 0);
		if (!advancedOptionsButton.isVisible()) {
			advancedOptions.setVisible(false);
		}
		advancedOptionsButton.setText(advancedOptions.isVisible() ?  msg.getMessage("Fido.advancedOptions.hide") : msg.getMessage("Fido.advancedOptions"));
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

	long getNonDeletedKeysNumber()
	{
		return credentials.stream().filter(c -> c.getState() != FidoCredentialInfoWrapper.CredentialState.DELETED).count();
	}

	public void setCredentialError(EngineException error)
	{
		if (nonNull(error))
			fidoComponent.showError("Error", error.getLocalizedMessage());
	}
}
