/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;

import java.util.function.Function;

public class DialogWithActionFooter extends Dialog
{
	private final Button actionButton;
	private final Button cancelButton;

	public DialogWithActionFooter(Function<String, String> msg)
	{
		setModal(true);
		cancelButton = new Button(msg.apply("cancel"), e -> close());
		actionButton = new Button(msg.apply("ok"));
		actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		getFooter().add(cancelButton, actionButton);
	}

	public void setActionButton(String msg, Runnable onConfirm)
	{
		actionButton.setText(msg);
		actionButton.addClickListener(e -> onConfirm.run());
	}

	public void setCancelButton(String msg, Runnable onCancel)
	{
		cancelButton.setText(msg);
		cancelButton.addClickListener(e -> onCancel.run());
	}

	public void setCancelButtonVisible(boolean value)
	{
		cancelButton.setVisible(value);
	}

	public void setActionButtonVisible(boolean value)
	{
		actionButton.setVisible(value);
	}

	public void addActionButtonListener(Runnable runnable)
	{
		actionButton.addClickListener(e -> runnable.run());
	}
}
