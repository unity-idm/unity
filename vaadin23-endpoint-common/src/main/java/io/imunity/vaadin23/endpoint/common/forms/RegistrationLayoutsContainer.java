/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.endpoint.common.forms;


import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class RegistrationLayoutsContainer
{
	public final VerticalLayout registrationFormLayout;

	public RegistrationLayoutsContainer(Float formLayoutWidth, Unit formLayoutWidthUnit)
	{
		this.registrationFormLayout = new VerticalLayout();
		this.registrationFormLayout.setWidth(formLayoutWidth, formLayoutWidthUnit);
		this.registrationFormLayout.setMargin(false);
		this.registrationFormLayout.setPadding(false);
		this.registrationFormLayout.getStyle().set("gap", "0");
	}

	public void addFormLayoutToRootLayout(VerticalLayout main)
	{
		main.add(registrationFormLayout);
		main.setAlignItems(FlexComponent.Alignment.CENTER);
	}
}
