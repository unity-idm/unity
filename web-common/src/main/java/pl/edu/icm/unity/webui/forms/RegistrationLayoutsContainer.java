/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;

/**
 * Holds the registration form layout.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class RegistrationLayoutsContainer
{
	public final VerticalLayout registrationFormLayout;

	public RegistrationLayoutsContainer(Float formLayoutWidth, Unit formLayoutWidthUnit)
	{
		this.registrationFormLayout = new VerticalLayout();
		this.registrationFormLayout.setWidth(formLayoutWidth, formLayoutWidthUnit);
		this.registrationFormLayout.setMargin(false);
	}

	public void addFormLayoutToRootLayout(VerticalLayout main)
	{
		main.addComponent(registrationFormLayout);
		main.setComponentAlignment(registrationFormLayout, Alignment.MIDDLE_CENTER);
	}
}
