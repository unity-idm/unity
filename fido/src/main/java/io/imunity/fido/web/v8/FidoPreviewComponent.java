/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web.v8;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.Styles;

import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.isNull;

/**
 * Displays information about single FidoCredential public key.
 *
 * @author R. Ledzinski
 */
class FidoPreviewComponent extends CustomComponent
{
	public static final int CAPTION_WIDTH = 125;
	private final MessageSource msg;
	private final FidoCredentialInfoWrapper credential;

	public FidoPreviewComponent(final MessageSource msg,
								final FidoCredentialInfoWrapper credential,
								final Runnable deleteCallback)
	{
		super();
		this.msg = msg;
		this.credential = credential;

		setCompositionRoot(getMainForm(deleteCallback));
	}

	private Layout getMainForm(final Runnable deleteCallback)
	{
		VerticalLayout root = new VerticalLayout();
		root.setSpacing(false);
		root.setMargin(false);

		root.addComponent(addLine(msg.getMessage("Fido.authenticatorInfo"),
				Optional.ofNullable(credential.getCredential().getDescription()).orElse(""),
				0,
				credential::setDescription));

		root.addComponent(addLine(msg.getMessage("Fido.attestationType"),
				msg.getMessage("Fido." + credential.getCredential().getAttestationFormat()),
				CAPTION_WIDTH,
				null));

		root.addComponent(addLine(msg.getMessage("Fido.created"),
				msg.getMessage("Fido.createdFormat", credential.getRegistrationTimestamp()),
				CAPTION_WIDTH,
				null));

		Button action = new Button(msg.getMessage("Fido.deleteKey"));
		action.setStyleName(Styles.vButtonLink.toString());
		action.addClickListener(event ->
		{
			credential.setState(credential.isDeleted() ? FidoCredentialInfoWrapper.CredentialState.NEW : FidoCredentialInfoWrapper.CredentialState.DELETED);
			deleteCallback.run();
		});
		root.addComponent(action);
		root.setComponentAlignment(action, Alignment.MIDDLE_RIGHT);

		return root;
	}

	private Layout addLine(String name, String value, int width, Consumer<String> setter)
	{
		HorizontalLayout layout = new HorizontalLayout();
		layout.setMargin(false);

		Label caption = new Label();
		caption.setValue(name);
		caption.addStyleName("u-bold");
		if (width > 0)
		{
			caption.setWidth(width, Unit.PIXELS);
		}
		layout.addComponent(caption);
		if (isNull(setter))
		{
			Label label = new Label();
			label.setValue(value);
			layout.addComponent(label);
			layout.setComponentAlignment(label, Alignment.MIDDLE_LEFT);
		} else
		{
			TextField textField = new TextField();
			textField.setValue(value);
			layout.addComponent(textField);
			layout.setComponentAlignment(textField, Alignment.MIDDLE_RIGHT);
			layout.setExpandRatio(textField, 1);
			textField.addValueChangeListener(event -> setter.accept(event.getValue()));
		}

		layout.setComponentAlignment(caption, Alignment.MIDDLE_RIGHT);
		return layout;
	}
}
