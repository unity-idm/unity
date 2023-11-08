/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry;


import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

@PrototypeComponent
public class RegistrationFormEditor extends VerticalLayout
{
	public void setForm(RegistrationForm deepCopy)
	{

	}

	public RegistrationFormEditor init(boolean b)
	{
		return this;
	}
}
