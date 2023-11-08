/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;

import pl.edu.icm.unity.base.message.MessageSource;

public class ShowViewActionLayoutFactory
{
	public static HorizontalLayout buildTopButtonsBar(Button... butttons)
	{
		HorizontalLayout headerLayout = new HorizontalLayout();
		headerLayout.setPadding(false);
		headerLayout.setMargin(false);
		headerLayout.setSpacing(true);
		headerLayout.setWidthFull();
		headerLayout.add(butttons);
		headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		headerLayout.setAlignSelf(Alignment.END, butttons);
		return headerLayout;
	}
	
	public static Button buildActionButton(String caption, VaadinIcon icon, ComponentEventListener<ClickEvent<Button>> clickListener)
	{
		Button button = buildButton(caption, icon, clickListener);
		button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		return button;
	}
	
	public static Button buildButton(String caption, VaadinIcon icon, ComponentEventListener<ClickEvent<Button>> clickListener)
	{
		Button button = new Button();
		button.setIcon(icon.create());
		button.setText(caption);
		button.addClickListener(clickListener);
		return button;
	}
	
	public static Button build4AddAction(MessageSource msg, ComponentEventListener<ClickEvent<Button>> clickListener)
	{
		return buildActionButton(msg.getMessage("addNew"), VaadinIcon.PLUS_CIRCLE_O, clickListener);
	}
}
