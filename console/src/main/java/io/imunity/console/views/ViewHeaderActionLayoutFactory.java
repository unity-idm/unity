/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.base.message.MessageSource;

public class ViewHeaderActionLayoutFactory
{
	public static VerticalLayout createHeaderActionLayout(MessageSource msg, Class<? extends ConsoleViewComponent> editRedirectClass)
	{
		VerticalLayout headerLayout = new VerticalLayout();
		headerLayout.setPadding(false);
		headerLayout.setSpacing(false);
		Button addButton = new Button(msg.getMessage("addNew"), e -> UI.getCurrent().navigate(editRedirectClass));
		addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		addButton.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
		headerLayout.setAlignItems(FlexComponent.Alignment.END);
		headerLayout.add(addButton);
		return headerLayout;
	}
}
