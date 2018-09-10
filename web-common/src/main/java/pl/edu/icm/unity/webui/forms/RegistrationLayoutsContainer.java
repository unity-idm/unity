/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;

/**
 * Holds the registration form layouts. At the point of creation of this
 * container, the layouts are not yet embedded into the
 * {@link BaseRequestEditor}. Requires calling
 * {@link BaseRequestEditor#finalizeLayoutInitialization(RegistrationLayoutsContainer)}.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class RegistrationLayoutsContainer
{
	public final VerticalLayout mainLayout;
	public final FormLayout registrationFormLayout;

	public RegistrationLayoutsContainer(VerticalLayout rootLayout, FormLayout formLayout)
	{
		this.mainLayout = rootLayout;
		this.registrationFormLayout = formLayout;
	}
}
