/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import pl.edu.icm.unity.base.message.MessageSource;

public class EditViewActionLayoutFactory
{
	public static HorizontalLayout createActionLayout(MessageSource msg, boolean editMode,
			Class<? extends ConsoleViewComponent> closeRedirectClass, Runnable onConfirm)
	{
		Button cancelButton = new Button(msg.getMessage("cancel"));
		cancelButton.addClickListener(event -> UI.getCurrent().navigate(closeRedirectClass));
		cancelButton.setWidthFull();
		Button updateButton = new Button(editMode ? msg.getMessage("update") : msg.getMessage("create"));
		updateButton.addClickListener(event -> onConfirm.run());
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		updateButton.setWidthFull();
		HorizontalLayout buttonsLayout = new HorizontalLayout(cancelButton, updateButton);
		buttonsLayout.setClassName("u-edit-view-action-buttons-layout");
		return buttonsLayout;
	}
	
	public static HorizontalLayout createActionLayout(MessageSource msg, String actionButton,
			Class<? extends ConsoleViewComponent> closeRedirectClass, Runnable onConfirm)
	{
		Button cancelButton = new Button(msg.getMessage("cancel"));
		cancelButton.addClickListener(event -> UI.getCurrent().navigate(closeRedirectClass));
		cancelButton.setWidthFull();
		Button updateButton = new Button(actionButton);
		updateButton.addClickListener(event -> onConfirm.run());
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		updateButton.setWidthFull();
		HorizontalLayout buttonsLayout = new HorizontalLayout(cancelButton, updateButton);
		buttonsLayout.setClassName("u-edit-view-action-buttons-layout");
		return buttonsLayout;
	}

	public static HorizontalLayout createActionLayout(MessageSource msg, Class<? extends ConsoleViewComponent> closeRedirectClass)
	{
		Button cancelButton = new Button(msg.getMessage("cancel"));
		cancelButton.addClickListener(event -> UI.getCurrent().navigate(closeRedirectClass));
		cancelButton.setWidthFull();
		HorizontalLayout buttonsLayout = new HorizontalLayout(cancelButton);
		buttonsLayout.setClassName("u-edit-view-action-buttons-layout");
		return buttonsLayout;
	}
}
