/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import pl.edu.icm.unity.base.message.MessageSource;

import java.util.Optional;

class FidoPreviewComponent extends VerticalLayout
{
	public static final int CAPTION_WIDTH = 125;
	private final MessageSource msg;
	private final FidoCredentialInfoWrapper credential;

	public FidoPreviewComponent(MessageSource msg,
	                            FidoCredentialInfoWrapper credential,
	                            Runnable deleteCallback)
	{
		this.msg = msg;
		this.credential = credential;

		setMargin(false);
		setPadding(false);
		init(deleteCallback);
	}

	private void init(Runnable deleteCallback)
	{
		TextField textField = new TextField(msg.getMessage("Fido.authenticatorInfo"));
		textField.setValue(Optional.ofNullable(credential.getCredential().getDescription()).orElse(""));
		textField.addValueChangeListener(event -> credential.setDescription(event.getValue()));
		textField.setWidthFull();
		textField.setRequired(true);
		add(textField);

		add(addLine(msg.getMessage("Fido.attestationType"),
				msg.getMessage("Fido." + credential.getCredential().getAttestationFormat())
		));

		add(addLine(msg.getMessage("Fido.created"),
				msg.getMessage("Fido.createdFormat", credential.getRegistrationTimestamp())
		));

		Button action = new Button(msg.getMessage("Fido.deleteKey"));
		action.addClickListener(event ->
		{
			credential.setState(credential.isDeleted() ? FidoCredentialInfoWrapper.CredentialState.NEW : FidoCredentialInfoWrapper.CredentialState.DELETED);
			deleteCallback.run();
		});
		action.getStyle().set("align-self", "center");
		add(action);
	}

	private HorizontalLayout addLine(String name, String value)
	{
		HorizontalLayout layout = new HorizontalLayout();
		layout.setMargin(false);

		Label caption = new Label();
		caption.setText(name);
		caption.getStyle().set("font-weight", "bold");
		caption.setWidth(FidoPreviewComponent.CAPTION_WIDTH, Unit.PIXELS);

		layout.add(caption);
		Label label = new Label();
		label.setText(value);
		layout.add(label);
		layout.setAlignItems(Alignment.CENTER);

		layout.setAlignItems(FlexComponent.Alignment.CENTER);
		return layout;
	}
}
