/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry;

import pl.edu.icm.unity.base.registration.RegistrationForm;

public class RegistrationFormChangedEvent extends BaseFormChangedEvent<RegistrationForm>
{
	public RegistrationFormChangedEvent(String name)
	{
		super(name);
	}

	public RegistrationFormChangedEvent(RegistrationForm form)
	{
		super(form);
	}
}
