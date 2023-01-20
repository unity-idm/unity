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
import pl.edu.icm.unity.MessageSource;

import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.isNull;

class FidoPreviewComponent extends VerticalLayout
{
	public static final int CAPTION_WIDTH = 125;
	private final MessageSource msg;
	private final FidoCredentialInfoWrapper credential;

	public FidoPreviewComponent(final MessageSource msg,
	                            final FidoCredentialInfoWrapper credential,
	                            final Runnable deleteCallback)
	{
		this.msg = msg;
		this.credential = credential;

		add(getMainForm(deleteCallback));
	}

	private VerticalLayout getMainForm(final Runnable deleteCallback)
	{
		VerticalLayout root = new VerticalLayout();
		root.setSpacing(false);
		root.setMargin(false);

		root.add(addLine(msg.getMessage("Fido.authenticatorInfo"),
				Optional.ofNullable(credential.getCredential().getDescription()).orElse(""),
				0,
				credential::setDescription));

		root.add(addLine(msg.getMessage("Fido.attestationType"),
				msg.getMessage("Fido." + credential.getCredential().getAttestationFormat()),
				CAPTION_WIDTH,
				null));

		root.add(addLine(msg.getMessage("Fido.created"),
				msg.getMessage("Fido.createdFormat", credential.getRegistrationTimestamp()),
				CAPTION_WIDTH,
				null));

		Button action = new Button(msg.getMessage("Fido.deleteKey"));
		action.addClickListener(event ->
		{
			credential.setState(credential.isDeleted() ? FidoCredentialInfoWrapper.CredentialState.NEW : FidoCredentialInfoWrapper.CredentialState.DELETED);
			deleteCallback.run();
		});
		root.add(action);
		root.setAlignItems(FlexComponent.Alignment.CENTER);

		return root;
	}

	private HorizontalLayout addLine(String name, String value, int width, Consumer<String> setter)
	{
		HorizontalLayout layout = new HorizontalLayout();
		layout.setMargin(false);

		Label caption = new Label();
		caption.setText(name);
		caption.getStyle().set("font-weight", "bold");
		if (width > 0)
		{
			caption.setWidth(width, Unit.PIXELS);
		}
		layout.add(caption);
		if (isNull(setter))
		{
			Label label = new Label();
			label.setText(value);
			layout.add(label);
			layout.setAlignItems(Alignment.CENTER);
		} else
		{
			TextField textField = new TextField();
			textField.setValue(value);
			layout.add(textField);
			layout.setAlignItems(Alignment.CENTER);
			textField.addValueChangeListener(event -> setter.accept(event.getValue()));
		}

		layout.setAlignItems(FlexComponent.Alignment.CENTER);
		return layout;
	}
}
